package no.nav.veilarbaktivitet.oppfolging.oppfolgingsperiode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT15M")
@Slf4j
@RequiredArgsConstructor
public class OppfolgingsperiodeCron {
    private final OppfolgingsperiodeService oppfolgingsperiodeServiceAdder;

    @Scheduled(
            initialDelayString = "${app.env.scheduled.default.initialDelay}",
            fixedDelayString = "${app.env.scheduled.oppfolgingsperiode.fixedDelay}"
    )
    @SchedulerLock(name = "addOppfolgingsperioder_scheduledTask", lockAtMostFor = "PT10M")
    public void addOppfolgingsperioder() {
        long antall = oppfolgingsperiodeServiceAdder.oppdater500brukere();
        log.info("oppdatert {} brukere med oppfolginsperiode", antall);
    }

    @Scheduled(
            cron = "0 0 22 16 * *"
    ) //TODO bare kjøres en gang husk og slette før neste månde
    @SchedulerLock(name = "addOppfolgingsperioder_scheduledTask", lockAtMostFor = "PT20H")
    public void sammkjorOppfolignsperiode() {
        oppfolgingsperiodeServiceAdder.samskjorAktiviter(10_000);
    }

}
