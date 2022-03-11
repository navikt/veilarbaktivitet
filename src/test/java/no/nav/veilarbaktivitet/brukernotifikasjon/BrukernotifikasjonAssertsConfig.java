package no.nav.veilarbaktivitet.brukernotifikasjon;

import lombok.Getter;
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput;
import no.nav.brukernotifikasjon.schemas.input.DoneInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import no.nav.veilarbaktivitet.brukernotifikasjon.avslutt.AvsluttBrukernotifikasjonCron;
import no.nav.veilarbaktivitet.brukernotifikasjon.oppgave.SendOppgaveCron;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaAvroTemplate;
import no.nav.veilarbaktivitet.util.KafkaTestService;
import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Getter
public class BrukernotifikasjonAssertsConfig {
    @Autowired
    private KafkaTestService testService;

    @Value("${topic.ut.brukernotifikasjon.oppgave}")
    private String oppgaveTopic;

    @Value("${topic.ut.brukernotifikasjon.beskjed}")
    private String beskjedTopic;

    @Value("${topic.ut.brukernotifikasjon.done}")
    private String brukernotifkasjonFerdigTopic;

    @Value("${topic.inn.eksternVarselKvittering}")
    private String kviteringsToppic;

    @Autowired
    private KafkaAvroTemplate<DoknotifikasjonStatus> kviteringsProducer;

    @Value("${app.env.appname}")
    private String appname;
    @Value("${app.env.namespace}")
    private String namespace;

    @Autowired
    private AvsluttBrukernotifikasjonCron avsluttBrukernotifikasjonCron;

    @Autowired
    private SendOppgaveCron sendOppgaveCron;

    Consumer<NokkelInput, OppgaveInput> createOppgaveConsumer() {
         return testService.createAvroAvroConsumer(oppgaveTopic);
    }
    Consumer<NokkelInput, BeskjedInput> createBeskjedConsumer() {
         return testService.createAvroAvroConsumer(beskjedTopic);
    }
    Consumer<NokkelInput, DoneInput> createDoneConsumer() {
         return testService.createAvroAvroConsumer(brukernotifkasjonFerdigTopic);
    }

}
