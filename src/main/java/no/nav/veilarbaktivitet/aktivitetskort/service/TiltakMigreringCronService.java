package no.nav.veilarbaktivitet.aktivitetskort.service;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.util.DateUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TiltakMigreringCronService {

    private final UnleashClient unleashClient;

    private final TiltakMigreringDAO tiltakMigreringDAO;

    private final AktivitetDAO aktivitetDAO;

    public static final String TILTAK_HISTORISK_MIGRERING_CRON_TOGGLE = "veilarbaktivitet.tiltakmigrering.cron.disabled";

    @Scheduled(
            initialDelayString = "${app.env.scheduled.default.initialDelay}",
            fixedDelayString = "${app.env.scheduled.default.fixedDelay}"
    )
    @SchedulerLock(name = "historiske_tiltak_migrering_cron", lockAtMostFor = "PT20M")
    public void settTiltakOpprettetSomHistoriskTilHistorisk() {
        if (unleashClient.isEnabled(TILTAK_HISTORISK_MIGRERING_CRON_TOGGLE)) {
            return;
        }

        List<AktivitetData> aktiviteter = tiltakMigreringDAO.hentTiltakOpprettetSomHistoriskSomIkkeErHistorisk(500);

        aktiviteter
                .stream()
                .map(a -> a
                        .withTransaksjonsType(AktivitetTransaksjonsType.BLE_HISTORISK)
                        .withHistoriskDato(DateUtils.localDateTimeToDate(a.getEksternAktivitetData().getOppfolgingsperiodeSlutt())))
                .forEach(aktivitetDAO::oppdaterAktivitet);
    }

}
