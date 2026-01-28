package no.nav.veilarbaktivitet.brukernotifikasjon

import io.micrometer.core.annotation.Timed
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO
import no.nav.veilarbaktivitet.arena.model.ArenaId
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.AktivitetVarsel
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.ArenaAktivitetVarsel
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.MinSideVarselId
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.UtgåendeVarsel
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.SkalSendes
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.VarselDAO
import no.nav.veilarbaktivitet.config.TeamLog.teamLog
import no.nav.veilarbaktivitet.manuell_status.v2.ManuellStatusV2Client
import no.nav.veilarbaktivitet.oppfolging.periode.SistePeriodeService
import no.nav.veilarbaktivitet.person.Person
import no.nav.veilarbaktivitet.person.Person.Fnr
import no.nav.veilarbaktivitet.person.PersonService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.*
import kotlin.jvm.optionals.getOrNull

@Service
open class MinsideVarselService(
    val minsideVarselProducer: MinsideVarselProducer,
    val varselDAO: VarselDAO,
    val aktivitetDao: AktivitetDAO,
    val manuellStatusClient: ManuellStatusV2Client,
    val personService: PersonService,
    @Value("\${app.env.aktivitetsplan.basepath}")
    val aktivitetsplanBasepath: String,
    var sistePeriodeService: SistePeriodeService
) {
    private val log = LoggerFactory.getLogger(MinsideVarselService::class.java)

    open fun hentVarselSomSkalSendes(maxAntall: Int): List<SkalSendes> = varselDAO.hentVarselSomSkalSendes(maxAntall)
    open fun avbrytIkkeSendteOppgaverForAvslutteteAktiviteter(): Int = varselDAO.avbrytIkkeSendteOppgaverForAvslutteteAktiviteter()

    open fun finnesBrukernotifikasjonMedVarselTypeForAktivitet(aktivitetsId: Long, varselType: VarselType): Boolean {
        return varselDAO.finnesBrukernotifikasjonMedVarselTypeForAktivitet(aktivitetsId, varselType)
    }
    open fun setDone(aktivitetId: Long, varseltype: VarselType) = varselDAO.setDone(aktivitetId, varseltype)
    open fun setDone(aktivitetId: ArenaId, varseltype: VarselType) = varselDAO.setDone(aktivitetId, varseltype)
    open fun setDoneGrupperingsID(uuid: UUID) = varselDAO.setDoneGrupperingsID(uuid)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Timed(value = "brukernotifikasjon_opprett_oppgave_sendt")
    open fun send(skalSendes: SkalSendes) {
        val oppdatertOk = varselDAO.setSendt(skalSendes.brukernotifikasjonLopeNummer)
        if (oppdatertOk) {
            sendVarsel(skalSendes)
        }
    }

    private fun sendVarsel(skalSendes: SkalSendes) {
        val offset: Long = minsideVarselProducer.send(skalSendes)
        log.info(
            "Minside varsel publisert på kafka med offset={} med type={} varselId={}",
            offset,
            skalSendes.varselType.brukernotifikasjonType.name,
            skalSendes.varselId.toString(),
        )
    }

    open fun kanVarsles(aktorId: Person.AktorId): Boolean {
        val manuellStatusResponse = manuellStatusClient.get(aktorId)
        val erManuell = manuellStatusResponse.map { it.isErUnderManuellOppfolging }.orElse(true)
        val erReservertIKrr = manuellStatusResponse.map { it.krrStatus }.map { it.isErReservert }.orElse(true)

        val kanVarsles = !erManuell && !erReservertIKrr
        if (!kanVarsles) {
            teamLog.info(
                "bruker kan ikke varsles aktorId: {}, erManuell: {}, erReservertIKrr: {}",
                aktorId.get(),
                erManuell,
                erReservertIKrr
            )
        }
        return kanVarsles
    }

    @Transactional
    open fun opprettVarselPaaAktivitet(varsel: AktivitetVarsel): MinSideVarselId {
        val fnr = personService.getFnrForAktorId(varsel.aktorId)
        val gjeldendeOppfolgingsperiode = sistePeriodeService.hentGjeldendeOppfolgingsperiodeMedFallback(varsel.aktorId)
        val (varselId, lopeNr) = lagreIOutbox(varsel, gjeldendeOppfolgingsperiode, fnr)
        log.info("Minside varsel lagret i outbox varselId={} type={} aktivitetid={}", varselId, varsel.varseltype, varsel.aktivitetId)
        varselDAO.kobleAktivitetIdTilBrukernotifikasjon(lopeNr, varsel.aktivitetId, varsel.aktitetVersion)
        return varselId
    }

    @Transactional
    open fun opprettVarselPaaArenaAktivitet(
        varsel: ArenaAktivitetVarsel
    ): MinSideVarselId {
        val aktorId = personService
            .getAktorIdForPersonBruker(varsel.fnr)
            .orElseThrow()
        val gjeldendeOppfolgingsperiode = sistePeriodeService.hentGjeldendeOppfolgingsperiodeMedFallback(aktorId)

        // epostTittel, epostBody og smsTekst settes til standartekst av brukernotifiaksjoenr hvis ikke satt
        val (varselId, lopeNr) = lagreIOutbox(varsel, gjeldendeOppfolgingsperiode)
        log.info("Minside varsel lagret i outbox varselId={} type={} aktivitetId={} arenaAktivitetid={}", varselId, VarselType.FORHAANDSORENTERING, varsel.aktivitetId.getOrNull(), varsel.arenaAktivitetId)

        varselDAO.kobleArenaAktivitetIdTilBrukernotifikasjon(lopeNr, varsel.arenaAktivitetId)
        // Populer brukernotifikasjon koblingstabell til vanlig aktivitet også
        varsel.aktivitetId
            .flatMap { id -> aktivitetDao.hentMaybeAktivitet(id) }
            .ifPresent { aktivitet ->
                varselDAO.kobleAktivitetIdTilBrukernotifikasjon(
                    lopeNr,
                    aktivitet.id,
                    aktivitet.versjon
                )
            }

        return varselId
    }

    private fun nyMinsideVarselId(): MinSideVarselId {
        return MinSideVarselId(UUID.randomUUID())
    }

    private fun lagreIOutbox(arenaAktivitetVarsel: ArenaAktivitetVarsel, oppfolgingsPeriode: UUID): VarselIdOgLopeNr {
        return lagreIOutbox(arenaAktivitetVarsel.toUgåendeVarsel(
            nyMinsideVarselId(),
            oppfolgingsPeriode,
            aktivitetsplanBasepath
        ))
    }

    private fun lagreIOutbox(aktivitetVarsel: AktivitetVarsel, oppfolgingsPeriode: UUID, fnr: Fnr): VarselIdOgLopeNr {
        return lagreIOutbox(aktivitetVarsel.toUgåendeVarsel(
            nyMinsideVarselId(),
            oppfolgingsPeriode,
            aktivitetsplanBasepath,
            fnr
        ))
    }

    private fun lagreIOutbox(utgåendeVarsel: UtgåendeVarsel): VarselIdOgLopeNr {
        // epostTittel, epostBody og smsTekst settes til standartekst av brukernotifiaksjoenr hvis ikke satt
        val brukernotifikasjonId = varselDAO.opprettBrukernotifikasjonIOutbox(utgåendeVarsel)
        return  VarselIdOgLopeNr(utgåendeVarsel.varselId, brukernotifikasjonId)
    }
}

data class VarselIdOgLopeNr(
    val varselId: MinSideVarselId,
    val lopeNr: Long
)