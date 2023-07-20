package no.nav.veilarbaktivitet.aktivitetskort.service

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.common.featuretoggle.UnleashClient
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType
import no.nav.veilarbaktivitet.util.DateUtils
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

@Component
open class TiltakMigreringCronService(
    val unleashClient: UnleashClient,
    val tiltakMigreringDAO: TiltakMigreringDAO,
    val aktivitetDAO: AktivitetDAO,
) {

    @Scheduled(
        initialDelayString = "\${app.env.scheduled.default.initialDelay}",
        fixedDelayString = "\${app.env.scheduled.default.fixedDelay}"
    )
    @SchedulerLock(name = "historiske_tiltak_migrering_cron", lockAtMostFor = "PT20M")
    open fun settTiltakOpprettetSomHistoriskTilHistorisk() {
        if (unleashClient.isEnabled(TILTAK_HISTORISK_MIGRERING_CRON_TOGGLE)) return
        tiltakMigreringDAO.hentTiltakOpprettetSomHistoriskSomIkkeErHistorisk(500)
            .map { aktivitet -> aktivitet
                    .withTransaksjonsType(AktivitetTransaksjonsType.BLE_HISTORISK)
                    .withHistoriskDato(DateUtils.localDateTimeToDate(aktivitet.getEksternAktivitetData().oppfolgingsperiodeSlutt))
            }
            .forEach { aktivitet: AktivitetData? -> aktivitetDAO.oppdaterAktivitet(aktivitet) }
    }

    companion object {
        const val TILTAK_HISTORISK_MIGRERING_CRON_TOGGLE = "veilarbaktivitet.tiltakmigrering.cron.disabled"
    }
}
