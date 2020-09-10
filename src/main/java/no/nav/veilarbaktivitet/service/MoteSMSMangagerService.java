package no.nav.veilarbaktivitet.service;


import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.db.dao.MoteSmsDAO;
import no.nav.veilarbaktivitet.domain.SmsAktivitetData;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static no.nav.veilarbaktivitet.util.DateUtils.omTimer;

@Component
@Slf4j
public class MoteSMSMangagerService {

    private final MoteSmsSenderService moteSmsSenderService;
    private final MoteSmsDAO moteSmsDAO;

    private final MeterRegistry registry;
    private final Counter antalSMSFeilet;
    private final Counter antellGangerGjenomfort;
    private AtomicLong smserSendt;
    private AtomicLong aktivterSendtSmsPaa;

    public MoteSMSMangagerService(
            MoteSmsSenderService moteSmsSenderService,
            MoteSmsDAO moteSmsDAO,
            MeterRegistry registry
    ) {
        this.moteSmsSenderService = moteSmsSenderService;
        this.moteSmsDAO = moteSmsDAO;

        this.registry = registry;
        antalSMSFeilet = registry.counter("antalSMSFeilet");
        antellGangerGjenomfort = registry.counter("antellGangerGjenomfort");
    }

    @Timed(value = "moteservicemlding", longTask = true, histogram = true)
    public void servicemeldingForMoterNesteDogn() {
        servicemeldingForMoter(omTimer(1), omTimer(24));
    }

    protected void servicemeldingForMoter(Date fra, Date til) {
        List<SmsAktivitetData> smsAktivitetData = moteSmsDAO.hentIkkeAvbrutteMoterMellom(fra, til);

        log.info("moteSMS antall hentet: " + smsAktivitetData.size());

        smsAktivitetData.stream()
                .filter(SmsAktivitetData::skalSendeServicevarsel)
                .forEach(this::trySendServicemelding);

        oppdaterMoteSmsMetrikker();
        antellGangerGjenomfort.increment();
        log.info("mote sms ferdig");
    }

    private void trySendServicemelding(SmsAktivitetData aktivitetData) {
        try {
            moteSmsSenderService.sendServiceMelding(aktivitetData);
        } catch (Exception e) {
            log.error("feil med varsel paa motesms for aktivitetId " + aktivitetData.getAktivitetId(), e);
            antalSMSFeilet.increment();
        }
    }

    private void oppdaterMoteSmsMetrikker() {
        long oppdatertSmsSendt = moteSmsDAO.antallSmsSendt();
        long oppdatertAktivterSendtSmsPaa = moteSmsDAO.antallAktivteterSendtSmsPaa();

        if(smserSendt == null) {
            smserSendt = registry.gauge("antallSmsSendt", new AtomicLong(oppdatertSmsSendt));
        } else {
            smserSendt.set(oppdatertSmsSendt);
        }

        if(aktivterSendtSmsPaa == null) {
            aktivterSendtSmsPaa = registry.gauge("antallAkteterSendtSmsPaa", new AtomicLong(oppdatertAktivterSendtSmsPaa));
        } else {
            aktivterSendtSmsPaa.set(oppdatertSmsSendt);
        }
    }
}
