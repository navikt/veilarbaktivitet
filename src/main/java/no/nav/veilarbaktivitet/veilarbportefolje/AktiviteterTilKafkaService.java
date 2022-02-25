package no.nav.veilarbaktivitet.veilarbportefolje;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.nav.common.featuretoggle.UnleashClient;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

import static no.nav.veilarbaktivitet.config.CronService.STOPP_AKTIVITETER_TIL_KAFKA;

@Service
@Slf4j
@RequiredArgsConstructor
public class AktiviteterTilKafkaService {
    private final KafkaAktivitetDAO dao;
    private final AktivitetKafkaProducerService producerService;
    private final UnleashClient unleashClient;

    @Scheduled(
            initialDelayString = "${app.env.scheduled.portefolje.initialDelay}",
            fixedDelayString = "${app.env.scheduled.portefolje.fixedDelay}"
    )
    @SchedulerLock(name = "aktiviteter_kafka_scheduledTask", lockAtMostFor = "PT2M")
    @Timed
    public void sendOppTil5000AktiviterTilPortefolje() {
        MDC.put("running.job", "aktiviteter_kafka");

        if(!unleashClient.isEnabled(STOPP_AKTIVITETER_TIL_KAFKA)) {
            List<KafkaAktivitetMeldingV4> meldinger = dao.hentOppTil5000MeldingerSomIkkeErSendtPaAiven();
            for (KafkaAktivitetMeldingV4 melding : meldinger) {
                producerService.sendAktivitetMelding(melding);
            }
        }

        MDC.clear();
    }

}
