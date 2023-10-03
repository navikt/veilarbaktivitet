package no.nav.veilarbaktivitet.veilarbportefolje;

import io.micrometer.core.annotation.Timed;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.nav.veilarbaktivitet.util.ExcludeFromCoverageGenerated;
import no.nav.veilarbaktivitet.veilarbportefolje.dto.KafkaAktivitetMeldingV4;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


@Service
@Slf4j
@RequiredArgsConstructor
public class AktiviteterTilKafkaService {
    private final KafkaAktivitetDAO dao;
    private final AktivitetKafkaProducerService producerService;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);



    @Scheduled(
            initialDelayString = "${app.env.scheduled.portefolje.initialDelay}",
            fixedDelayString = "${app.env.scheduled.portefolje.fixedDelay}"
    )
    @SchedulerLock(name = "aktiviteter_kafka_scheduledTask", lockAtMostFor = "PT2M")
    @Timed
    public void sendOppTil5000AktiviterTilPortefolje() {
        MDC.put("running.job", "aktiviteter_kafka");
        List<KafkaAktivitetMeldingV4> meldinger = dao.hentOppTil5000MeldingerSomIkkeErSendtPaAiven();
        for (KafkaAktivitetMeldingV4 melding : meldinger) {
            producerService.sendAktivitetMelding(melding);
        }
        MDC.clear();
    }
    @PreDestroy
    @ExcludeFromCoverageGenerated
    public void stopScheduler() {
        scheduledExecutorService.shutdown();
    }
}
