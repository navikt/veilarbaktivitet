package no.nav.veilarbaktivitet.avtaltMedNav.varsel;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.melding.virksomhet.stopprevarsel.v1.stopprevarsel.ObjectFactory;
import no.nav.melding.virksomhet.stopprevarsel.v1.stopprevarsel.StoppReVarsel;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBContext;
import static no.nav.veilarbaktivitet.util.MessageQueueUtils.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class StopRevarslingService {

    public static final JAXBContext STOPP_VARSEL_CONTEXT = jaxbContext(StoppReVarsel.class);

    private final JmsTemplate stopVarselQueue;
    //TODO private final VarselDAO varselDAO;

    void stopRevarsel(String varselUUID) {
        try {
            stopp(varselUUID);
        } catch (Exception e) {
            log.error("Feilet å sende stopp revarsel for: " + varselUUID, e);
        }
    }

    @SneakyThrows
    private void stopp(String varselUUID) {
        StoppReVarsel stoppReVarsel = new StoppReVarsel();
        stoppReVarsel.setVarselbestillingId(varselUUID);
        String melding = marshall(new ObjectFactory().createStoppReVarsel(stoppReVarsel), STOPP_VARSEL_CONTEXT);

        stopVarselQueue.send(messageCreator(melding, varselUUID));
        //TODO varselDAO.markerSomStoppet(varselUUID);
    }
}
