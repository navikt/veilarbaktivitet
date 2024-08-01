package no.nav.veilarbaktivitet.stilling_fra_nav

import io.micrometer.core.annotation.Timed
import lombok.RequiredArgsConstructor
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
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
        val sisteProsesserteVersjonFørBatch = batchTrackingDao.hentSisteProsseserteVersjon(BatchJob.Deling_av_cv_avbrutt_eller_fuulfort_uten_svar)
        val aktivitetData = delingAvCvDAO.hentStillingFraNavSomErFullfortEllerAvbruttUtenSvar(maxantall.toLong(), sisteProsesserteVersjonFørBatch)
        val prosesseringer = aktivitetData.map { aktivitet: AktivitetData ->
            try {
                delingAvCvService.notifiserAvbruttEllerFullfortUtenSvar(aktivitet)
                return@map BatchResult.Success(aktivitet.versjon)
            } catch (e: Exception) {
                log.warn("Behandling av fullført/avbrutt aktivitet aktivitetId=${aktivitet.id} feilet")
                log.error("Kunne ikke behandle avbrutt/fullført aktivitet", e)
                return@map BatchResult.Failure(aktivitet.versjon)
            }
        }
        val sisteProsesserteVersjonEtterBatch =  prosesseringer.sisteProsesserteVersjon(fallbackOffset = sisteProsesserteVersjonFørBatch)
        batchTrackingDao.setSisteProsesserteVersjon(BatchJob.Deling_av_cv_avbrutt_eller_fuulfort_uten_svar, sisteProsesserteVersjonEtterBatch)
        return aktivitetData.size
    }
}
