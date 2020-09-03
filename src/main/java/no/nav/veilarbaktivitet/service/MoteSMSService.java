package no.nav.veilarbaktivitet.service;


import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.leaderelection.LeaderElectionClient;
import no.nav.veilarbaktivitet.db.dao.MoteSmsDAO;
import no.nav.veilarbaktivitet.domain.SmsAktivitetData;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLAktoerId;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLParameter;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLVarsel;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLVarslingstyper;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBContext;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static java.util.UUID.randomUUID;
import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;
import static no.nav.veilarbaktivitet.service.MessageQueueUtils.*;
import static no.nav.veilarbaktivitet.util.DateUtils.omTimer;

@Component
@EnableScheduling
@Slf4j
public class MoteSMSService {

    private static final java.util.Locale norge = new java.util.Locale("no");
    private static final SimpleDateFormat DatoFormaterer = new SimpleDateFormat("dd. MMMM yyyy", norge);
    private static final SimpleDateFormat KlokkeFormaterer = new SimpleDateFormat("hh:mm", norge);
    private static final String AKTIVITETSPLAN_URL = getRequiredProperty("AKTIVITETSPLAN_URL");


    private final JmsTemplate varselQueue;

    private final MoteSmsDAO moteSmsDAO;

    private final UnleashService unleash;

    private final LeaderElectionClient leaderElectionClient;

    private final MeterRegistry registry;

    public MoteSMSService(JmsTemplate varselQueue,
                          MoteSmsDAO moteSmsDAO,
                          UnleashService unleash,
                          LeaderElectionClient leaderElectionClient,
                          MeterRegistry registry) {
        this.varselQueue = varselQueue;
        this.moteSmsDAO = moteSmsDAO;
        this.unleash = unleash;
        this.leaderElectionClient = leaderElectionClient;
        this.registry = registry;
    }

    @Scheduled(cron = "0 0/2 * * * *")
    public void sendSms() {

        boolean leader = leaderElectionClient.isLeader();
        log.info("motesms er ledaer : " + leader);

        if (leader) {
            faktiskSendSms();
        }
    }

    private void faktiskSendSms() {
        List<SmsAktivitetData> smsAktivitetData = moteSmsDAO.hentIkkeAvbrutteMoterMellom(omTimer(1), omTimer(24));
        Stream<SmsAktivitetData> aktiviteter = smsAktivitetData.stream();

        Stream<SmsAktivitetData> filtrerte = aktiviteter
                .filter(a -> !a.getMoteTidAktivitet().equals(a.getSmsSendtMoteTid()));

        boolean enabled = unleash.isEnabled("veilarbaktivitet.motesms");

        log.info("er moteSMS skrudd paa: " + enabled);
        log.info("moteSMS antallHentet: " + smsAktivitetData.size());
        log.info("moteSMS antallFiltrerte: " + filtrerte.count());

        registry.counter("moterHentetTilSMSFiltrering").increment(aktiviteter.count());
        Counter moteSMSSendt = registry.counter("moteSMSSendt");
        Counter moteSMSOppdatert = registry.counter("moteSMSOppdatert");
        registry.gauge("moteSMSSistStartet", new Date().getTime());

        if (enabled) {
            log.info("sender meldinger");
            filtrerte.forEach(
                    aktivitetData -> {
                        String varselId = randomUUID().toString();
                        String aktor = aktivitetData.getAktorId();
                        String moteTid = formaterDato(aktivitetData.getMoteTidAktivitet());
                        String url = AKTIVITETSPLAN_URL + "/aktivitet/vis/" + aktivitetData.getAktivitetId();

                        sendVarsel(aktor, url, moteTid, varselId);
                        moteSmsDAO.insertSmsSendt(aktivitetData.getAktivitetId(), aktivitetData.getAktivtetVersion(), aktivitetData.getMoteTidAktivitet(), varselId);

                        moteSMSSendt.increment();
                        if (aktivitetData.getMoteTidAktivitet() != null) {
                            moteSMSOppdatert.increment();
                        }
                    }
            );
        }
        registry.gauge("moteSMSSistSluttet", new Date().getTime());
    }

    private String formaterDato(Date date) {
        return DatoFormaterer.format(date) + " klokken " + KlokkeFormaterer.format(date) + ".";
    }


    private static final String VARSEL_ID = "AktivitetsplanMoteVarsel";

    private static final JAXBContext VARSEL_CONTEXT = jaxbContext(XMLVarsel.class, XMLVarslingstyper.class);

    private void sendVarsel(String aktorId, String url, String motedato, String varselId) {
        XMLVarsel xmlVarsel = new XMLVarsel()
                .withMottaker(new XMLAktoerId().withAktoerId(aktorId))
                .withVarslingstype(new XMLVarslingstyper(VARSEL_ID, null, null))
                .withParameterListes(
                        new XMLParameter("motedato", motedato),
                        new XMLParameter("aktiviteturl", url)
                );
        ;
        String message = marshall(xmlVarsel, VARSEL_CONTEXT);
        MessageCreator messageCreator = messageCreator(message, varselId);

        varselQueue.send(messageCreator);
    }
}
