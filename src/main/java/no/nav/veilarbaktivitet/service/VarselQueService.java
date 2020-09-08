package no.nav.veilarbaktivitet.service;

import no.nav.melding.virksomhet.varsel.v1.varsel.XMLAktoerId;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLParameter;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLVarsel;
import no.nav.melding.virksomhet.varsel.v1.varsel.XMLVarslingstyper;
import no.nav.veilarbaktivitet.domain.SmsAktivitetData;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBContext;
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.util.UUID.randomUUID;
import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;
import static no.nav.veilarbaktivitet.service.MessageQueueUtils.*;


@Component
public class VarselQueService {

    private final JmsTemplate varselQueue;

    private static final String AKTIVITETSPLAN_URL = getRequiredProperty("AKTIVITETSPLAN_URL");
    private static final String VARSEL_ID = "AktivitetsplanMoteVarsel";
    private static final JAXBContext VARSEL_CONTEXT = jaxbContext(XMLVarsel.class, XMLVarslingstyper.class);

    private static final java.util.Locale norge = new java.util.Locale("no");
    private static final SimpleDateFormat DatoFormaterer = new SimpleDateFormat("dd. MMMM yyyy", norge);
    private static final SimpleDateFormat KlokkeFormaterer = new SimpleDateFormat("HH:mm", norge);

    public VarselQueService(JmsTemplate varselQueue) {
        this.varselQueue = varselQueue;
    }

    public String sendMoteSms(SmsAktivitetData aktivitetData) {
        String varselId = randomUUID().toString();
        MessageCreator message = byggVarsel(varselId, aktivitetData);
        varselQueue.send(message);
        return varselId;
    }



    private static String formaterDato(Date date) {
        return DatoFormaterer.format(date) + " klokken " + KlokkeFormaterer.format(date);
    }


    private static MessageCreator byggVarsel(String varselId, SmsAktivitetData aktivitetData) {
        String aktorId = aktivitetData.getAktorId();
        String moteTid = formaterDato(aktivitetData.getMoteTidAktivitet());
        String url = AKTIVITETSPLAN_URL + "/aktivitet/vis/" + aktivitetData.getAktivitetId();

        XMLVarsel xmlVarsel = new XMLVarsel()
                .withMottaker(new XMLAktoerId().withAktoerId(aktorId))
                .withVarslingstype(new XMLVarslingstyper(VARSEL_ID, null, null))
                .withParameterListes(
                        new XMLParameter("motedato", moteTid),
                        new XMLParameter("aktiviteturl", url)
                );
        ;
        String message = marshall(xmlVarsel, VARSEL_CONTEXT);
        return messageCreator(message, varselId);
    }
}
