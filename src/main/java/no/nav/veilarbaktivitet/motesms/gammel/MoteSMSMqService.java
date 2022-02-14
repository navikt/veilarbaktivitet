package no.nav.veilarbaktivitet.motesms.gammel;


import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.UUID.randomUUID;
import static no.nav.veilarbaktivitet.util.DateUtils.omTimer;

@Slf4j
@Service
public class MoteSMSMqService {
    private final MoteSmsMqDAO moteSmsMqDAO;
    private final VarselQueueService varselQueue;
    private final TransactionTemplate transactionTemplate;


    private final MeterRegistry registry;
    private final Counter antalSMSFeilet;
    private final Counter antellGangerGjenomfort;
    private AtomicLong smserSendt;
    private AtomicLong aktivterSendtSmsPaa;

    public MoteSMSMqService(
            MoteSmsMqDAO moteSmsMqDAO,
            VarselQueueService varselQueue,
            PlatformTransactionManager platformTransactionManager,
            MeterRegistry registry
    ) {
        this.moteSmsMqDAO = moteSmsMqDAO;
        this.varselQueue = varselQueue;
        this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
        this.registry = registry;

        antalSMSFeilet = registry.counter("antalSMSFeilet");
        antellGangerGjenomfort = registry.counter("antellGangerGjenomfort");
    }

    @Timed(value = "moteservicemelding", histogram = true)
    public void sendServicemeldingerForNesteDogn() {
        sendServicemeldinger(omTimer(1), omTimer(24));
    }

    public void slattoppdatertMotesSmsMedGammel() {
        moteSmsMqDAO.hentIkkeAvbrutteMoterMellomMedGamelNotifikasjon(omTimer(1), omTimer(24))
                .stream()
                .map(SmsAktivitetData::getAktivitetId)
                .forEach(it -> moteSmsMqDAO.slettGjeldende(it));
    }

    protected void sendServicemeldinger(Date fra, Date til) {
        List<SmsAktivitetData> smsAktivitetData = moteSmsMqDAO.hentIkkeAvbrutteMoterMellom(fra, til);

        if (!smsAktivitetData.isEmpty()) {
            log.info("moteSMS antall hentet: " + smsAktivitetData.size());
        }

        smsAktivitetData.stream()
                .filter(SmsAktivitetData::skalSendeServicevarsel)
                .forEach(this::trySendServicemelding);

        oppdaterMoteSmsMetrikker();
        antellGangerGjenomfort.increment();

    }

    private void trySendServicemelding(SmsAktivitetData aktivitetData) {
        try {
            sendServiceMelding(aktivitetData);
        } catch (Exception e) {
            log.error("feil med varsel paa motesms for aktivitetId " + aktivitetData.getAktivitetId(), e);
            antalSMSFeilet.increment();
        }
    }

    private void sendServiceMelding(SmsAktivitetData aktivitetData) {
        transactionTemplate.executeWithoutResult(aktivitetdata -> {
            String varselId = randomUUID().toString();
            moteSmsMqDAO.insertSmsSendt(aktivitetData, varselId);
            varselQueue.sendMoteSms(aktivitetData, varselId);
        });
    }

    private void oppdaterMoteSmsMetrikker() {
        long oppdatertSmsSendt = moteSmsMqDAO.antallSmsSendt();
        long oppdatertAktivterSendtSmsPaa = moteSmsMqDAO.antallAktivteterSendtSmsPaa();

        if (smserSendt == null) {
            smserSendt = registry.gauge("antallSmsSendt", new AtomicLong(oppdatertSmsSendt));
        } else {
            smserSendt.set(oppdatertSmsSendt);
        }

        if (aktivterSendtSmsPaa == null) {
            aktivterSendtSmsPaa = registry.gauge("antallAktiviteterSendtSmsPaa", new AtomicLong(oppdatertAktivterSendtSmsPaa));
        } else {
            aktivterSendtSmsPaa.set(oppdatertSmsSendt);
        }
    }
}
