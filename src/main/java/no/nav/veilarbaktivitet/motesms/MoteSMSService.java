package no.nav.veilarbaktivitet.motesms;


import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import no.nav.veilarbaktivitet.db.dao.MoteSmsDAO;
import no.nav.veilarbaktivitet.domain.SmsAktivitetData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.UUID.randomUUID;
import static no.nav.veilarbaktivitet.util.DateUtils.omTimer;

@Slf4j
@Service
public class MoteSMSService implements HealthCheck {
    private final MoteSmsDAO moteSmsDAO;
    private final VarselQueueService varselQueue;
    private final TransactionTemplate transactionTemplate;


    private final MeterRegistry registry;
    private final Counter antalSMSFeilet;
    private final Counter antellGangerGjenomfort;
    private AtomicLong smserSendt;
    private AtomicLong aktivterSendtSmsPaa;
    private final AtomicBoolean healty = new AtomicBoolean(true);
    private final AtomicBoolean healtyThisRound = new AtomicBoolean(true);

    public MoteSMSService(
            MoteSmsDAO moteSmsDAO,
            VarselQueueService varselQueue,
            PlatformTransactionManager platformTransactionManager,
            MeterRegistry registry
    ) {
        this.moteSmsDAO = moteSmsDAO;
        this.varselQueue = varselQueue;
        this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
        this.registry = registry;

        antalSMSFeilet = registry.counter("antalSMSFeilet");
        antellGangerGjenomfort = registry.counter("antellGangerGjenomfort");
    }

    @Timed(value = "moteservicemlding", longTask = true, histogram = true)
    public void sendServicemeldingerForNesteDogn() {
        sendServicemeldinger(omTimer(1), omTimer(24));
    }

    protected void sendServicemeldinger(Date fra, Date til) {
        List<SmsAktivitetData> smsAktivitetData = moteSmsDAO.hentIkkeAvbrutteMoterMellom(fra, til);

        log.info("moteSMS antall hentet: " + smsAktivitetData.size());

        smsAktivitetData.stream()
                .filter(SmsAktivitetData::skalSendeServicevarsel)
                .forEach(this::trySendServicemelding);

        oppdaterMoteSmsMetrikker();
        antellGangerGjenomfort.increment();
        log.info("mote sms ferdig");

        healty.set(healtyThisRound.get());
        healtyThisRound.set(true);
    }

    private void trySendServicemelding(SmsAktivitetData aktivitetData) {
        try {
            sendServiceMelding(aktivitetData);
        } catch (Exception e) {
            log.error("feil med varsel paa motesms for aktivitetId " + aktivitetData.getAktivitetId(), e);
            antalSMSFeilet.increment();
            healty.set(false);
            healtyThisRound.set(false);
        }
    }

    private void sendServiceMelding(SmsAktivitetData aktivitetData) {
        transactionTemplate.executeWithoutResult(aktivitetdata -> {
            String varselId = randomUUID().toString();
            moteSmsDAO.insertSmsSendt(aktivitetData, varselId);
            varselQueue.sendMoteSms(aktivitetData, varselId);
        });
    }

    private void oppdaterMoteSmsMetrikker() {
        long oppdatertSmsSendt = moteSmsDAO.antallSmsSendt();
        long oppdatertAktivterSendtSmsPaa = moteSmsDAO.antallAktivteterSendtSmsPaa();

        if (smserSendt == null) {
            smserSendt = registry.gauge("antallSmsSendt", new AtomicLong(oppdatertSmsSendt));
        } else {
            smserSendt.set(oppdatertSmsSendt);
        }

        if (aktivterSendtSmsPaa == null) {
            aktivterSendtSmsPaa = registry.gauge("antallAkteterSendtSmsPaa", new AtomicLong(oppdatertAktivterSendtSmsPaa));
        } else {
            aktivterSendtSmsPaa.set(oppdatertSmsSendt);
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        return healty.get() ? HealthCheckResult.healthy() : HealthCheckResult.unhealthy("Sending av motesms feiler");
    }
}
