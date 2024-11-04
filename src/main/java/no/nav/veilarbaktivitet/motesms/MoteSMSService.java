package no.nav.veilarbaktivitet.motesms;

import io.micrometer.core.annotation.Timed;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.nav.veilarbaktivitet.brukernotifikasjon.MinsideVarselService;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType;
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.AktivitetVarsel;
import no.nav.veilarbaktivitet.util.ExcludeFromCoverageGenerated;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.time.Duration.ofHours;

@Service
@Slf4j
@RequiredArgsConstructor
public class MoteSMSService {
    private final MoteSmsDAO moteSmsDAO;
    private final MinsideVarselService brukernotifikasjonService;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    @Scheduled(
            initialDelayString = "${app.env.scheduled.default.initialDelay}",
            fixedDelayString = "${app.env.scheduled.default.fixedDelay}"
    )
    @SchedulerLock(name = "send_mote_sms_scheduledTask", lockAtMostFor = "PT20M")
    @Timed(value = "moteservicemelding", histogram = true)
    public void sendMoteSms() {
        MDC.put("running.job", "moteSmsService");
        sendServicemeldinger(ofHours(1), ofHours(24));
        MDC.clear();
    }

    protected void sendServicemeldinger(Duration fra, Duration til) {
        moteSmsDAO.hentMoterUtenVarsel(fra, til, 5000)
                .forEach(it -> {
                    if (brukernotifikasjonService.kanVarsles(it.aktorId())) {
                        var varsel = new AktivitetVarsel(
                                it.aktivitetId(),
                                it.aktitetVersion(),
                                it.aktorId(),
                                it.getDitNavTekst(),
                                VarselType.MOTE_SMS,
                                it.getEpostTitel(),
                                it.getEpostBody(),
                                it.getSmsTekst()
                        );
                        var varselId = brukernotifikasjonService.opprettVarselPaaAktivitet(varsel);
                        moteSmsDAO.insertGjeldendeSms(it, varselId);
                    } else {
                        // Usikker på hvorfor man inserter i gjeldende sms når bruker ikke kan
                        // varsles men det var gjort slik tidligere
                        moteSmsDAO.insertGjeldendeSms(it, null);
                        log.info("Minside varsel ikke sendt (møte sms), bruker kan ikke varsles {}", it.aktorId());
                    }
                });
    }

    @Scheduled(
            initialDelayString = "${app.env.scheduled.default.initialDelay}",
            fixedDelayString = "${app.env.scheduled.default.fixedDelay}"
    )
    @SchedulerLock(name = "stopp_mote_sms_scheduledTask", lockAtMostFor = "PT20M")
    @Timed(value = "stopmoteservicemelding", histogram = true)
    public void stopMoteSms() {
        MDC.put("running.job", "moteSmsServiceStopper");

        moteSmsDAO.hentMoterMedOppdatertTidEllerKanal(5000)
                .forEach(it -> {
                    brukernotifikasjonService.setDone(it, VarselType.MOTE_SMS);
                    moteSmsDAO.slettGjeldende(it); //TODO endre til send beskjed sms om flyttet møte + skal sende på nytt hvis møtet er mere enn 48 timer fremm i tid
                });

        moteSmsDAO.hentMoteSmsSomFantStedForMerEnd(Duration.ofDays(7)) //TODO Trenger vi denne? Holder det at bruker kan fjerne den og den forsvinner når aktiviteter er fulført/avbrut eller blir historisk
                .forEach(it -> {
                    brukernotifikasjonService.setDone(it, VarselType.MOTE_SMS);
                    moteSmsDAO.slettGjeldende(it);
                });

        MDC.clear();
    }
    @PreDestroy
    @ExcludeFromCoverageGenerated
    public void stopScheduler() {
        scheduledExecutorService.shutdown();
    }
}
