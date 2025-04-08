package no.nav.veilarbaktivitet.aktivitetskort.service

import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO
import no.nav.veilarbaktivitet.aktivitet.AktivitetService
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.Ident
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetsMessageDAO
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortCompareUtil
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortConsumer.OffsetAndPartition
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortMapper.toAktivitetsDataInsert
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortMapper.toAktivitetsDataUpdate
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.AktivitetskortBestilling
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.ArenaAktivitetskortBestilling
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.EksternAktivitetskortBestilling
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.MessageSource
import no.nav.veilarbaktivitet.aktivitetskort.feil.AktivitetsKortFunksjonellException
import no.nav.veilarbaktivitet.aktivitetskort.feil.ManglerOppfolgingsperiodeFeil
import no.nav.veilarbaktivitet.aktivitetskort.feil.UlovligEndringFeil
import no.nav.veilarbaktivitet.oppfolging.periode.IngenGjeldendePeriodeException
import no.nav.veilarbaktivitet.oppfolging.periode.SistePeriodeService
import no.nav.veilarbaktivitet.util.DateUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class AktivitetskortService(
    private val aktivitetService: AktivitetService,
    private val aktivitetDAO: AktivitetDAO,
    private val aktivitetsMessageDAO: AktivitetsMessageDAO,
    private val arenaAktivitetskortService: ArenaAktivitetskortService,
    private val sistePeriodeService: SistePeriodeService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    @Throws(AktivitetsKortFunksjonellException::class)
    fun upsertAktivitetskort(bestilling: AktivitetskortBestilling): UpsertActionResult {
        val (id) = bestilling.aktivitetskort
        // Splittede aktiviteter bestilles på ny funksjonell-id
        val gammelAktivitetMaybe = aktivitetDAO.hentAktivitetByFunksjonellId(id)
        return when {
            gammelAktivitetMaybe.isPresent -> {
                val gammelAktivitet = gammelAktivitetMaybe.get()
                val nyAktivitet = bestilling.toAktivitetsDataUpdate()
                // Arenaaktiviteter blir vanlig "ekstern"-aktivitet etter de har blitt opprettet
                val oppdatertAktivitet = when  {
                    bestilling is ArenaAktivitetskortBestilling && gammelAktivitet.erTattOverAvAnnetTeam() -> {
                        // Vi gjør arena-migrering men beholder andre team sine data, ikke data fra ACL, dette er pga en race-condition
                        // hvor andre team behandler endringer fra arena raskere enn oss
                        arenaAktivitetskortService.dobbelsjekkMigrering(bestilling, gammelAktivitet)
                        return UpsertActionResult.IGNORER
                    }
                    bestilling is ArenaAktivitetskortBestilling -> oppdaterAktivitet(gammelAktivitet, nyAktivitet, true)
                    else -> oppdaterAktivitet(gammelAktivitet, nyAktivitet, false)
                }
                log.info("Oppdaterte ekstern aktivitetskort {}", oppdatertAktivitet)
                UpsertActionResult.OPPDATER
            }
            else -> {
                val opprettetAktivitet = opprettAktivitet(bestilling)
                log.info("Opprettet ekstern aktivitetskort {}", opprettetAktivitet)
                UpsertActionResult.OPPRETT
            }
        }
    }

    @Throws(ManglerOppfolgingsperiodeFeil::class)
    private fun opprettAktivitet(bestilling: AktivitetskortBestilling): AktivitetData? {
        return when (bestilling) {
            is ArenaAktivitetskortBestilling -> arenaAktivitetskortService.opprettAktivitet(bestilling)
            is EksternAktivitetskortBestilling -> opprettEksternAktivitet(bestilling)
            else -> throw IllegalStateException("Unexpected value: $bestilling")
        }
    }

    @Throws(ManglerOppfolgingsperiodeFeil::class)
    private fun opprettEksternAktivitet(bestilling: EksternAktivitetskortBestilling): AktivitetData {
        val oppfolgingsperiode = try {
            sistePeriodeService.hentGjeldendeOppfolgingsperiodeMedFallback(bestilling.aktorId)
        } catch (e: IngenGjeldendePeriodeException) {
            throw ManglerOppfolgingsperiodeFeil()
        }
        val aktivitetData: AktivitetData = bestilling.toAktivitetsDataInsert()
        return aktivitetService.opprettAktivitet(
            aktivitetData.withOppfolgingsperiodeId(oppfolgingsperiode),
        )
    }

    private fun oppdaterDetaljer(aktivitet: AktivitetData, nyAktivitet: AktivitetData): AktivitetData {
        return if (AktivitetskortCompareUtil.erFaktiskOppdatert(nyAktivitet, aktivitet)) {
            aktivitetService.oppdaterAktivitet(
                aktivitet,
                nyAktivitet
            )
        } else aktivitet
    }

    fun oppdaterStatus(aktivitet: AktivitetData, nyAktivitet: AktivitetData): AktivitetData {
        return if (aktivitet.status != nyAktivitet.status) {
            aktivitetService.oppdaterStatus(
                aktivitet,
                nyAktivitet
            )
        } else {
            aktivitet
        }
    }

    @Throws(UlovligEndringFeil::class)
    private fun oppdaterAktivitet(gammelAktivitet: AktivitetData, nyAktivitet: AktivitetData, arenaAclOppdatering: Boolean): AktivitetData {
        if (gammelAktivitet.aktorId != nyAktivitet.aktorId) throw UlovligEndringFeil("Kan ikke endre bruker på samme aktivitetskort")
        // Arena-ACL kan foreløpig oppdatere historiske kort
        if (gammelAktivitet.historiskDato != null && !arenaAclOppdatering) throw UlovligEndringFeil("Kan ikke endre aktiviteter som er historiske (avsluttet oppfølgingsperiode)")
        if (gammelAktivitet.isAvtalt && !nyAktivitet.isAvtalt) throw UlovligEndringFeil("Kan ikke oppdatere fra avtalt til ikke-avtalt")
        return gammelAktivitet
            .let { aktivitet: AktivitetData -> settAvtaltHvisAvtalt(aktivitet, nyAktivitet) }
            .let { aktivitet: AktivitetData -> oppdaterDetaljer(aktivitet, nyAktivitet) }
            .let { aktivitet: AktivitetData -> oppdaterStatus(aktivitet, nyAktivitet) }
    }

    private fun settAvtaltHvisAvtalt(originalAktivitet: AktivitetData, nyAktivitet: AktivitetData): AktivitetData {
        return if (nyAktivitet.isAvtalt && !originalAktivitet.isAvtalt) {
            aktivitetService.settAvtalt(
                originalAktivitet,
                Ident(nyAktivitet.endretAv, nyAktivitet.endretAvType),
                DateUtils.dateToLocalDateTime(nyAktivitet.endretDato)
            )
        } else {
            originalAktivitet
        }
    }

    fun harSettMelding(messageId: UUID): Boolean {
        return aktivitetsMessageDAO.exist(messageId)
    }

    fun lagreMeldingsId(messageId: UUID, funksjonellId: UUID, offsetAndPartition: OffsetAndPartition) {
        aktivitetsMessageDAO.insert(messageId, funksjonellId, offsetAndPartition)
    }

    fun oppdaterMeldingResultat(messageId: UUID, upsertActionResult: UpsertActionResult, reason: String?) {
        aktivitetsMessageDAO.updateActionResult(messageId, upsertActionResult, reason)
    }

    fun hentAktivitetskortByFunksjonellId(funksjonellId: UUID): Optional<AktivitetData> {
        return aktivitetDAO.hentAktivitetByFunksjonellId(funksjonellId)
    }
}

fun AktivitetData.erTattOverAvAnnetTeam() = this.eksternAktivitetData.source != MessageSource.ARENA_TILTAK_AKTIVITET_ACL.name