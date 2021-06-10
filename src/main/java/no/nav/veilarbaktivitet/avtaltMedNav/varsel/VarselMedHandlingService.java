package no.nav.veilarbaktivitet.avtaltMedNav.varsel;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.AktoerId;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.ObjectFactory;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.Parameter;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.VarselMedHandling;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import static no.nav.veilarbaktivitet.util.MessageQueueUtils.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class VarselMedHandlingService {

    private static final String PARAGAF8_VARSEL_ID = "DittNAV_000008";
    private static final JAXBContext VARSEL_MED_HANDLING = jaxbContext(ObjectFactory.class);

    private final JmsTemplate varselMedHandlingQueue;

    @SneakyThrows
    void send(String aktorId, String varselbestillingId) {
        AktoerId motaker = new AktoerId();
        motaker.setAktoerId(aktorId);
        VarselMedHandling varselMedHandling = new VarselMedHandling();
        varselMedHandling.setVarseltypeId(PARAGAF8_VARSEL_ID);
        varselMedHandling.setReVarsel(false);
        varselMedHandling.setMottaker(motaker);
        varselMedHandling.setVarselbestillingId(varselbestillingId);

        Parameter parameter = new Parameter();
        parameter.setKey("varselbestillingId");
        parameter.setValue(varselbestillingId);

        varselMedHandling
                .getParameterListe()
                .add(parameter);

        JAXBElement<VarselMedHandling> melding = new ObjectFactory().createVarselMedHandling(varselMedHandling);

        varselMedHandlingQueue.send(messageCreator(marshall(melding, VARSEL_MED_HANDLING), varselbestillingId));
    }

}
