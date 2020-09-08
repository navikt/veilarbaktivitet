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

import static no.nav.veilarbaktivitet.util.DateUtils.omTimer;

@Component
@Slf4j
public class MoteSMSService {

    private final VarselQueService varselQueue;
    private final MoteSmsDAO moteSmsDAO;

    private final MeterRegistry registry;

    public MoteSMSService(VarselQueService varselQueue,
                          MoteSmsDAO moteSmsDAO,
                          MeterRegistry registry) {
        this.varselQueue = varselQueue;
        this.moteSmsDAO = moteSmsDAO;
        this.registry = registry;
    }

    @Timed(value = "moteservicemlding", longTask = true)
    public void sendServicemelding() {
        sendServicemelding(omTimer(1), omTimer(24));
    }

    protected void sendServicemelding(Date fra, Date til) {
        List<SmsAktivitetData> smsAktivitetData = moteSmsDAO.hentIkkeAvbrutteMoterMellom(fra, til);

        log.info("moteSMS antall hentet: " + smsAktivitetData.size());

        registry.counter("moterHentetTilSMSFiltrering").increment(smsAktivitetData.size());
        Counter antallSMSSendt = registry.counter("antallSMSSendt");
        Counter antalSMSOppdatert = registry.counter("antalSMSOppdatert");
        registry.gauge("moteSMSSistStartet", new Date().getTime());

        smsAktivitetData.stream()
                .filter(SmsAktivitetData::skalSendeServicevarsel)
                .forEach(
                        aktivitetData -> {

                            String varselId = varselQueue.sendMoteSms(aktivitetData);
                            moteSmsDAO.insertSmsSendt(aktivitetData, varselId);

                            antallSMSSendt.increment();
                            if (aktivitetData.getSmsSendtMoteTid() != null) {
                                antalSMSOppdatert.increment();
                            }
                        }
                );

        registry.gauge("moteSMSSistSluttet", new Date().getTime());
        log.info("mote sms ferdig");
    }
}
