package no.nav.veilarbaktivitet.aktivitetskort.service

import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO
import no.nav.veilarbaktivitet.aktivitet.AktivitetService
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.Ident
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetsMessageDAO
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortCompareUtil
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortMapper.mapTilAktivitetData
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.AktivitetskortBestilling
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.ArenaAktivitetskortBestilling
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.EksternAktivitetskortBestilling
import no.nav.veilarbaktivitet.aktivitetskort.feil.AktivitetsKortFunksjonellException
import no.nav.veilarbaktivitet.aktivitetskort.feil.ManglerOppfolgingsperiodeFeil
import no.nav.veilarbaktivitet.aktivitetskort.feil.UlovligEndringFeil
import no.nav.veilarbaktivitet.person.Person
import no.nav.veilarbaktivitet.util.DateUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import java.util.stream.Stream

@Service
@Slf4j
class AktivitetskortService(
    private val aktivitetService: AktivitetService,
    private val aktivitetDAO: AktivitetDAO,
    private val aktivitetsMessageDAO: AktivitetsMessageDAO,
    private val arenaAktivitetskortService: ArenaAktivitetskortService,
    private val oppfolgingsperiodeService: OppfolgingsperiodeService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    @Throws(AktivitetsKortFunksjonellException::class)
    fun upsertAktivitetskort(bestilling: AktivitetskortBestilling): UpsertActionResult {
        val (id) = bestilling.aktivitetskort
        val gammelAktivitetVersjon = aktivitetDAO.hentAktivitetByFunksjonellId(id)
        return if (gammelAktivitetVersjon.isPresent) {
            // Arenaaktiviteter er blitt "ekstern"-aktivitet etter de har blitt opprettet
            val oppdatertAktivitet = oppdaterAktivitet(gammelAktivitetVersjon.get(), bestilling.toAktivitet())
            log.info("Oppdaterte ekstern aktivitetskort {}", oppdatertAktivitet)
            UpsertActionResult.OPPDATER
        } else {
            val opprettetAktivitet = opprettAktivitet(bestilling)
            log.info("Opprettet ekstern aktivitetskort {}", opprettetAktivitet)
            UpsertActionResult.OPPRETT
        }
    }

    @Throws(ManglerOppfolgingsperiodeFeil::class)
    private fun opprettAktivitet(bestilling: AktivitetskortBestilling): AktivitetData {
        return if (bestilling is ArenaAktivitetskortBestilling) {
            arenaAktivitetskortService.opprettAktivitet(bestilling)
        } else (bestilling as? EksternAktivitetskortBestilling)?.let { opprettEksternAktivitet(it) }
            ?: throw IllegalStateException("Unexpected value: $bestilling")
    }

    @Throws(ManglerOppfolgingsperiodeFeil::class)
    private fun opprettEksternAktivitet(bestilling: EksternAktivitetskortBestilling): AktivitetData {
        val endretAv = bestilling.aktivitetskort.endretAv
        val opprettet = bestilling.aktivitetskort.endretTidspunkt.toLocalDateTime()
        val oppfolgingsperiode = oppfolgingsperiodeService!!.finnOppfolgingsperiode(bestilling.aktorId, opprettet)
            ?: throw ManglerOppfolgingsperiodeFeil()
        val aktivitetData: AktivitetData = mapTilAktivitetData(bestilling, bestilling.aktivitetskort.endretTidspunkt, oppfolgingsperiode)

        return aktivitetService.opprettAktivitet(
            Person.aktorId(aktivitetData.aktorId),
            aktivitetData,
            endretAv,
            opprettet,
            oppfolgingsperiode.uuid
        )
    }

    private fun oppdaterDetaljer(aktivitet: AktivitetData, nyAktivitet: AktivitetData?): AktivitetData {
        return if (AktivitetskortCompareUtil.erFaktiskOppdatert(nyAktivitet, aktivitet)) {
            aktivitetService!!.oppdaterAktivitet(
                aktivitet,
                nyAktivitet,
                Person.navIdent(nyAktivitet!!.endretAv),
                DateUtils.dateToLocalDateTime(
                    nyAktivitet.endretDato
                )
            )
        } else aktivitet
    }

    fun oppdaterStatus(aktivitet: AktivitetData, nyAktivitet: AktivitetData): AktivitetData {
        return if (aktivitet.status != nyAktivitet.status) {
            aktivitetService!!.oppdaterStatus(
                aktivitet,
                nyAktivitet,  // TODO: Populer avbrutt-tekstfelt
                Ident(nyAktivitet.endretAv, nyAktivitet.endretAvType),
                DateUtils.dateToLocalDateTime(nyAktivitet.endretDato)
            )
        } else {
            aktivitet
        }
    }

    @Throws(UlovligEndringFeil::class)
    private fun oppdaterAktivitet(gammelAktivitet: AktivitetData, nyAktivitet: AktivitetData?): AktivitetData? {
        if (gammelAktivitet.aktorId != nyAktivitet!!.aktorId) throw UlovligEndringFeil("Kan ikke endre bruker på samme aktivitetskort")
        if (gammelAktivitet.historiskDato != null) throw UlovligEndringFeil("Kan ikke endre aktiviteter som er historiske (avsluttet oppfølgingsperiode)")
        //TODO vurder avtalt-logikken https://trello.com/c/dFyre4EK
        if (gammelAktivitet.avtalt && !nyAktivitet.avtalt) throw UlovligEndringFeil("Kan ikke oppdatere fra avtalt til ikke-avtalt")
        return Stream.of(gammelAktivitet)
            .map { aktivitet: AktivitetData -> settAvtaltHvisAvtalt(aktivitet, nyAktivitet) }
            .map { aktivitet: AktivitetData -> oppdaterDetaljer(aktivitet, nyAktivitet) }
            .map { aktivitet: AktivitetData -> oppdaterStatus(aktivitet, nyAktivitet) }
            .findFirst().orElse(null)
    }

    private fun settAvtaltHvisAvtalt(originalAktivitet: AktivitetData, nyAktivitet: AktivitetData): AktivitetData {
        return if (nyAktivitet.avtalt && !originalAktivitet.avtalt) {
            aktivitetService.settAvtalt(
                originalAktivitet,
                Ident(nyAktivitet.endretAv, nyAktivitet.endretAvType),
                DateUtils.dateToLocalDateTime(nyAktivitet.endretDato)
            )
        } else {
            originalAktivitet
        }
    }

    fun harSettMelding(messageId: UUID?): Boolean {
        return aktivitetsMessageDAO!!.exist(messageId!!)
    }

    fun lagreMeldingsId(messageId: UUID?, funksjonellId: UUID?) {
        aktivitetsMessageDAO!!.insert(messageId, funksjonellId)
    }

    fun oppdaterMeldingResultat(messageId: UUID?, upsertActionResult: UpsertActionResult?, reason: String?) {
        aktivitetsMessageDAO!!.updateActionResult(messageId, upsertActionResult, reason)
    }
}
