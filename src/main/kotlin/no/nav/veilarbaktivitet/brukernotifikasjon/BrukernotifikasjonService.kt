package no.nav.veilarbaktivitet.brukernotifikasjon

import io.micrometer.core.annotation.Timed
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO
import no.nav.veilarbaktivitet.arena.model.ArenaId
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.AktivitetVarsel
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.ArenaAktivitetVarsel
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.SkalSendes
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.VarselDAO
import no.nav.veilarbaktivitet.manuell_status.v2.ManuellStatusV2Client
import no.nav.veilarbaktivitet.oppfolging.periode.SistePeriodeService
import no.nav.veilarbaktivitet.person.Person
import no.nav.veilarbaktivitet.person.PersonService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
open class BrukernotifikasjonService(
    val brukernotifikasjonProducer: BrukernotifikasjonProducer,
    val varselDAO: VarselDAO,
    val brukerNotifikasjonDAO: BrukerNotifikasjonDAO,
    val aktivitetDao: AktivitetDAO,
    val manuellStatusClient: ManuellStatusV2Client,
    val personService: PersonService,
    @Value("\${app.env.aktivitetsplan.basepath}")
    val aktivitetsplanBasepath: String,
    var sistePeriodeService: SistePeriodeService
) {
    private val log = LoggerFactory.getLogger(BrukernotifikasjonService::class.java)
    private val secureLogs: Logger = LoggerFactory.getLogger("SecureLog")

    open fun hentVarselSomSkalSendes(maxAntall: Int): List<SkalSendes> = varselDAO.hentVarselSomSkalSendes(maxAntall)
    open fun avbrytIkkeSendteOppgaverForAvslutteteAktiviteter(): Int = varselDAO.avbrytIkkeSendteOppgaverForAvslutteteAktiviteter()

    open fun finnesBrukernotifikasjonMedVarselTypeForAktivitet(aktivitetsId: Long, varselType: VarselType): Boolean {
        return brukerNotifikasjonDAO.finnesBrukernotifikasjonMedVarselTypeForAktivitet(aktivitetsId, varselType)
    }
    open fun setDone(aktivitetId: Long, varseltype: VarselType) = brukerNotifikasjonDAO.setDone(aktivitetId, varseltype)
    open fun setDone(aktivitetId: ArenaId, varseltype: VarselType) = brukerNotifikasjonDAO.setDone(aktivitetId, varseltype)
    open fun setDoneGrupperingsID(uuid: UUID) = brukerNotifikasjonDAO.setDoneGrupperingsID(uuid)

    @Transactional
    @Timed(value = "brukernotifikasjon_opprett_oppgave_sendt")
    open fun send(skalSendes: SkalSendes) {
        val oppdatertOk = varselDAO.setSendt(skalSendes.brukernotifikasjonLopeNummer)
        if (oppdatertOk) {
            sendVarsel(skalSendes)
        }
    }

    private fun sendVarsel(skalSendes: SkalSendes) {
        val offset: Long = brukernotifikasjonProducer.send(skalSendes)
        log.debug(
            "Brukernotifikasjon {} med type {} publisert med offset {}",
            skalSendes.brukernotifikasjonId.toString(),
            skalSendes.varselType.brukernotifikasjonType.name,
            offset
        )
    }

    open fun kanVarsles(aktorId: Person.AktorId): Boolean {
        val manuellStatusResponse = manuellStatusClient.get(aktorId)
        val erManuell = manuellStatusResponse.map { it.isErUnderManuellOppfolging }.orElse(true)
        val erReservertIKrr = manuellStatusResponse.map { it.krrStatus }.map { it.isErReservert }.orElse(true)

        val kanVarsles = !erManuell && !erReservertIKrr
        if (!kanVarsles) {
            secureLogs.info(
                "bruker kan ikke varsles aktorId: {}, erManuell: {}, erReservertIKrr: {}",
                aktorId.get(),
                erManuell,
                erReservertIKrr
            )
        }
        return kanVarsles
    }

    @Transactional
    open fun opprettVarselPaaAktivitet(varsel: AktivitetVarsel): UUID {
        val uuid = UUID.randomUUID()
        val fnr = personService.getFnrForAktorId(varsel.aktorId)
        val gjeldendeOppfolgingsperiode = sistePeriodeService.hentGjeldendeOppfolgingsperiodeMedFallback(varsel.aktorId)
        val brukernotifikasjonId = brukerNotifikasjonDAO.opprettBrukernotifikasjonIOutbox(
            varsel.toUgåendeVarsel(
                uuid,
                gjeldendeOppfolgingsperiode,
                aktivitetsplanBasepath,
                fnr
            )
        )
        brukerNotifikasjonDAO.kobleAktivitetIdTilBrukernotifikasjon(brukernotifikasjonId, varsel.aktivitetId, varsel.aktitetVersion)
        return uuid
    }

    @Transactional
    open fun opprettVarselPaaArenaAktivitet(
        arenaAktivitetVarsel: ArenaAktivitetVarsel
    ): UUID {
        val uuid = UUID.randomUUID()
        val aktorId = personService
            .getAktorIdForPersonBruker(arenaAktivitetVarsel.fnr)
            .orElseThrow()
        val gjeldendeOppfolgingsperiode = sistePeriodeService.hentGjeldendeOppfolgingsperiodeMedFallback(aktorId)

        // epostTittel, epostBody og smsTekst settes til standartekst av brukernotifiaksjoenr hvis ikke satt
        val brukernotifikasjonId = brukerNotifikasjonDAO.opprettBrukernotifikasjonIOutbox(
            arenaAktivitetVarsel.toUgåendeVarsel(
                uuid,
                gjeldendeOppfolgingsperiode,
                aktivitetsplanBasepath,
            )
        )
        brukerNotifikasjonDAO.kobleArenaAktivitetIdTilBrukernotifikasjon(brukernotifikasjonId, arenaAktivitetVarsel.arenaAktivitetId)
        // Populer brukernotifikasjon koblingstabell til vanlig aktivitet også
        arenaAktivitetVarsel.aktivitetId
            .flatMap { id -> aktivitetDao.hentMaybeAktivitet(id) }
            .ifPresent { aktivitet ->
                brukerNotifikasjonDAO.kobleAktivitetIdTilBrukernotifikasjon(
                    brukernotifikasjonId,
                    aktivitet.id,
                    aktivitet.versjon
                )
            }

        return uuid
    }
}
