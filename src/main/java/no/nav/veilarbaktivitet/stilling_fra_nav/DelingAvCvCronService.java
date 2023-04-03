package no.nav.veilarbaktivitet.stilling_fra_nav;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.nav.veilarbaktivitet.util.ExcludeFromCoverageGenerated;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Service
@Slf4j
@RequiredArgsConstructor
public class DelingAvCvCronService {
    private final DelingAvCvFristUtloptService delingAvCvFristUtloptService;
    private final DelingAvCvManueltAvbruttService delingAvCvManueltAvbruttService;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    @Scheduled(
            initialDelayString = "${app.env.scheduled.default.initialDelay}",
            fixedDelayString = "${app.env.scheduled.default.fixedDelay}"
    )
    @SchedulerLock(name = "deling_av_cv_frinst_utlopt_avslutt", lockAtMostFor = "PT1H")
    void avsluttUtlopedeAktiviteter() {
        while (delingAvCvFristUtloptService.avsluttUtlopedeAktiviteter(500) == 500) ;
    }

    @Scheduled(
            initialDelayString = "${app.env.scheduled.default.initialDelay}",
            fixedDelayString = "${app.env.scheduled.default.fixedDelay}"
    )
    @SchedulerLock(name = "deling_av_cv_avbrutt_eller_fuulfort_uten_svar", lockAtMostFor = "PT1H")
    void notifiserAvbruttEllerFullfortUtenSvar() {
        while (delingAvCvManueltAvbruttService.notifiserFullfortEllerAvbruttUtenSvar(500) == 500) ;
    }
    @PreDestroy
    @ExcludeFromCoverageGenerated
    public void stopScheduler() {
        scheduledExecutorService.shutdown();
    }
}
