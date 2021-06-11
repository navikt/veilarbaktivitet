package no.nav.veilarbaktivitet.motesms;

import no.nav.veilarbaktivitet.domain.SmsAktivitetData;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Service;

@Service
public class VarselQueService {

    private final JmsTemplate varselQueue;

    private static final String VARSEL_ID = "AktivitetsplanMoteVarsel";
    private static final JAXBContext VARSEL_CONTEXT = jaxbContext(XMLVarsel.class, XMLVarslingstyper.class);

    public VarselQueService(JmsTemplate varselQueue) {
        this.varselQueue = varselQueue;
    }

    public void sendMoteSms(SmsAktivitetData aktivitetData, String varselId) {
        MessageCreator message = byggVarsel(varselId, aktivitetData);
        varselQueue.send(message);
    }

    private static MessageCreator byggVarsel(String varselId, SmsAktivitetData aktivitetData) {
        XMLVarsel xmlVarsel = new XMLVarsel()
                .withMottaker(new XMLAktoerId().withAktoerId(aktivitetData.getAktorId()))
                .withVarslingstype(new XMLVarslingstyper(VARSEL_ID, null, null))
                .withParameterListes(
                        new XMLParameter("motedato", aktivitetData.formatertMoteTid()),
                        new XMLParameter("motetype", aktivitetData.moteType()),
                        new XMLParameter("aktiviteturl", aktivitetData.url())
                );

        String message = marshall(xmlVarsel, VARSEL_CONTEXT);
        return messageCreator(message, varselId);
    }
}
