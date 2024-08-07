package no.nav.veilarbaktivitet.stilling_fra_nav

import io.micrometer.core.annotation.Timed
import lombok.RequiredArgsConstructor
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.util.BatchJob
import no.nav.veilarbaktivitet.util.BatchResult
import no.nav.veilarbaktivitet.util.BatchTrackingDAO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@RequiredArgsConstructor
open class DelingAvCvManueltAvbruttService(
    private val delingAvCvService: DelingAvCvService,
    private val delingAvCvDAO: DelingAvCvDAO,
    private val batchTrackingDao: BatchTrackingDAO
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Timed(value = "stillingFraNavAvbruttEllerFullfortUtenSvar", histogram = true)
    open fun notifiserFullfortEllerAvbruttUtenSvar(maxantall: Int): Int {
        return batchTrackingDao.withOffset(BatchJob.Deling_av_cv_avbrutt_eller_fuulfort_uten_svar) { sisteProsesserteVersjonFørBatch ->
            val aktivitetData = delingAvCvDAO.hentStillingFraNavSomErFullfortEllerAvbruttUtenSvar(maxantall.toLong(), sisteProsesserteVersjonFørBatch)
            aktivitetData.map { aktivitet: AktivitetData ->
                try {
                    delingAvCvService.notifiserAvbruttEllerFullfortUtenSvar(aktivitet)
                    return@map BatchResult.Success(aktivitet.versjon)
                } catch (e: Exception) {
                    log.warn("Behandling av fullført/avbrutt aktivitet aktivitetId=${aktivitet.id} feilet")
                    log.error("Kunne ikke behandle avbrutt/fullført aktivitet", e)
                    return@map BatchResult.Failure(aktivitet.versjon)
                }
            }
        }.size.also { log.info("Avsluttet $it brukernotifikasjoner for stilling-fra-nav aktiviteter som ble manuelt avbrutt/fullført") }
    }
}
