package no.nav.veilarbaktivitet.service;


import lombok.extern.slf4j.Slf4j;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.veilarbaktivitet.db.dao.MoteSmsDAO;
import no.nav.veilarbaktivitet.domain.SmsAktivitetData;
import no.nav.veilarbaktivitet.util.IsLeader;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLAktoerId;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLParameter;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLVarsel;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLVarslingstyper;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBContext;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static java.util.UUID.randomUUID;
import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;
import static no.nav.veilarbaktivitet.service.MessageQueueUtils.*;
import static no.nav.veilarbaktivitet.util.DateUtils.omTimer;

@Component
@Slf4j
public class MoteSMSService {

    private static final java.util.Locale norge = new java.util.Locale("no");
    private static final SimpleDateFormat DatoFormaterer = new SimpleDateFormat("dd. MMMM yyyy", norge);
    private static final SimpleDateFormat KlokkeFormaterer = new SimpleDateFormat("hh:mm", norge);
    private static final String AKTIVITETSPLAN_URL = getRequiredProperty("AKTIVITETSPLAN_URL");


    private final JmsTemplate varselQueue;

    private final MoteSmsDAO moteSmsDAO;

    private final UnleashService unleash;

    public MoteSMSService(JmsTemplate varselQueue, MoteSmsDAO moteSmsDAO, UnleashService unleash) {
        this.varselQueue = varselQueue;
        this.moteSmsDAO = moteSmsDAO;
        this.unleash = unleash;
    }

    @Scheduled(cron = "0 0/2 * * * *")
    public void sendSms() {
        if (IsLeader.isLeader()) {
            faktiskSendSms();
        }
    }

    private void faktiskSendSms() {
        List<SmsAktivitetData> aktiviteter = moteSmsDAO.hentMoterMellom(omTimer(1), omTimer(24));

        boolean enabled = unleash.isEnabled("veilarbaktivitet.motesms");

        log.info("aktiviteter hentet for mote sms" + aktiviteter.size());

        if(enabled) {
            log.info("sender meldinger");
            for (SmsAktivitetData aktivitetData : aktiviteter) {
                String varselId = randomUUID().toString();
                String aktor = aktivitetData.getAktorId();
                String moteTid = formaterDato(aktivitetData.getMoteTid());
                String url = AKTIVITETSPLAN_URL + "/aktivitet/vis/" + aktivitetData.getAktivitetId();

                sendVarsel(aktor, url, moteTid, varselId);
                moteSmsDAO.insertSmsSendt(aktivitetData.getAktivitetId(), aktivitetData.getAktivtetVersion(), aktivitetData.getMoteTid(), varselId);
            }
        }

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
