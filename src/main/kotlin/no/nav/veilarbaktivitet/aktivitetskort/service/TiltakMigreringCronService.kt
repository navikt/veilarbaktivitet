package no.nav.veilarbaktivitet.aktivitetskort.service

import io.getunleash.Unleash
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType
import no.nav.veilarbaktivitet.person.Innsender
import no.nav.veilarbaktivitet.util.DateUtils
import org.springframework.stereotype.Component
import java.util.*

@Component
open class TiltakMigreringCronService(
    val unleash: Unleash,
    val tiltakMigreringDAO: TiltakMigreringDAO,
    val aktivitetDAO: AktivitetDAO,
) {

    /* Trenger kun denne ved patching av historiske data fra arena */
//    @Scheduled(
//        initialDelayString = "\${app.env.scheduled.default.initialDelay}",
//        fixedDelayString = "\${app.env.scheduled.default.fixedDelay}",
//    )
//    @SchedulerLock(name = "historiske_tiltak_migrering_cron", lockAtMostFor = "PT20M")
    open fun settTiltakOpprettetSomHistoriskTilHistorisk() {
        if (unleash.isEnabled(TILTAK_HISTORISK_MIGRERING_CRON_TOGGLE)) return
        tiltakMigreringDAO.hentTiltakOpprettetSomHistoriskSomIkkeErHistorisk(500)
            .map { aktivitet -> aktivitet
                    .withTransaksjonsType(AktivitetTransaksjonsType.BLE_HISTORISK)
                    .withHistoriskDato(DateUtils.localDateTimeToDate(aktivitet.getEksternAktivitetData().oppfolgingsperiodeSlutt))
                    .withEndretDato(Date())
                    .withEndretAvType(Innsender.SYSTEM)
                    .withEndretAv("veilarbaktivitet")
            }
            .forEach { aktivitet: AktivitetData? -> aktivitetDAO.oppdaterAktivitet(aktivitet) }
    }

    companion object {
        const val TILTAK_HISTORISK_MIGRERING_CRON_TOGGLE = "veilarbaktivitet.tiltakmigrering.cron.disabled"
    }
}
