package no.nav.veilarbaktivitet.avtaltMedNav.varsel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.melding.virksomhet.opprettoppgavehenvendelse.v1.opprettoppgavehenvendelse.AktoerId;
import no.nav.melding.virksomhet.opprettoppgavehenvendelse.v1.opprettoppgavehenvendelse.OppgaveType;
import no.nav.melding.virksomhet.opprettoppgavehenvendelse.v1.opprettoppgavehenvendelse.Oppgavehenvendelse;
import no.nav.melding.virksomhet.stopprevarsel.v1.stopprevarsel.StoppReVarsel;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.ObjectFactory;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.Parameter;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.VarselMedHandling;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;

import static no.nav.veilarbaktivitet.util.MessageQueueUtils.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvtaltVarselMQClient {

    private static final String PARAGAF8_VARSEL_ID = "DittNAV_000008";

    private static final JAXBContext OPPGAVE_HENVENDELSE = jaxbContext(Oppgavehenvendelse.class);
    private static final JAXBContext VARSEL_MED_HANDLING = jaxbContext(ObjectFactory.class);
    private static final JAXBContext STOPP_VARSEL_CONTEXT = jaxbContext(StoppReVarsel.class);

    private final JmsTemplate oppgaveHenvendelseQueue;
    private final JmsTemplate varselMedHandlingQueue;
    private final JmsTemplate stopVarselQueue;

    void sendVarsel(String aktorId, String varselId, String aktivitetUrl) {
        sendVarselMedHandling(aktorId, varselId);
        sendOppgave(aktorId, varselId, aktivitetUrl);
    }

    void stopRevarsel(String varselUUID) {
        try {
            stopp(varselUUID);
        } catch (Exception e) {
            log.error("Feilet å sende stopp revarsel for: " + varselUUID, e);
        }
    }

    private void sendOppgave(String aktorId, String varselId, String aktivitetUrl) {
        MessageCreator messageCreator = messageCreator(marshall(createMelding(aktorId, varselId, aktivitetUrl), OPPGAVE_HENVENDELSE), varselId);
        oppgaveHenvendelseQueue.send(messageCreator);
    }

    private void sendVarselMedHandling(String aktorId, String varselId) {
        JAXBElement<VarselMedHandling> melding = createVarselMedHandling(aktorId, varselId);

        varselMedHandlingQueue.send(messageCreator(marshall(melding, VARSEL_MED_HANDLING), varselId));
    }

    private JAXBElement<Oppgavehenvendelse> createMelding(String aktorid, String uuid, String aktivitetUrl) {
        AktoerId aktoerId = new AktoerId();
        aktoerId.setAktoerId(aktorid);

        OppgaveType oppgaveType = new OppgaveType();
        oppgaveType.setValue("0004");

        Oppgavehenvendelse henvendelse = new Oppgavehenvendelse();
        henvendelse.setMottaker(aktoerId);
        henvendelse.setOppgaveType(oppgaveType);
        henvendelse.setVarselbestillingId(uuid);
        henvendelse.setOppgaveURL(aktivitetUrl);
        henvendelse.setStoppRepeterendeVarsel(false);

        return new no.nav.melding.virksomhet.opprettoppgavehenvendelse.v1.opprettoppgavehenvendelse.ObjectFactory().createOppgavehenvendelse(henvendelse);
    }

    private JAXBElement<VarselMedHandling> createVarselMedHandling(String aktorId, String varselbestillingId) {
        no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.AktoerId motaker = new no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.AktoerId();
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
        return melding;
    }

    private void stopp(String varselUUID) {
        StoppReVarsel stoppReVarsel = new StoppReVarsel();
        stoppReVarsel.setVarselbestillingId(varselUUID);
        String melding = marshall(new no.nav.melding.virksomhet.stopprevarsel.v1.stopprevarsel.ObjectFactory().createStoppReVarsel(stoppReVarsel), STOPP_VARSEL_CONTEXT);

        stopVarselQueue.send(messageCreator(melding, varselUUID));
    }

}
