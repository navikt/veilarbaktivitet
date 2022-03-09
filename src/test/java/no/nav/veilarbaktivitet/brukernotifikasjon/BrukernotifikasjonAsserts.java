package no.nav.veilarbaktivitet.brukernotifikasjon;

import no.nav.brukernotifikasjon.schemas.input.DoneInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.brukernotifikasjon.avslutt.AvsluttBrukernotifikasjonCron;
import no.nav.veilarbaktivitet.brukernotifikasjon.oppgave.SendOppgaveCron;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.util.KafkaTestService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static org.junit.Assert.assertEquals;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

@Service
public class BrukernotifikasjonAsserts {
    @Autowired
    KafkaTestService testService;

    @Value("${topic.ut.brukernotifikasjon.oppgave}")
    String oppgaveTopic;

    @Value("${topic.ut.brukernotifikasjon.beskjed}")
    String beskjedTopic;

    @Value("${topic.ut.brukernotifikasjon.done}")
    String brukernotifkasjonFerdigTopic;

    @Value("${app.env.appname}")
    private String appname;
    @Value("${app.env.namespace}")
    private String namespace;

    @Autowired
    private AvsluttBrukernotifikasjonCron avsluttBrukernotifikasjonCron;

    @Autowired
    SendOppgaveCron sendOppgaveCron;

    public ConsumerRecord<NokkelInput, OppgaveInput> oppgaveSendt(Person.Fnr fnr, AktivitetDTO aktivitetDTO) {
        Consumer<NokkelInput, OppgaveInput> avroAvroConsumer = testService.createAvroAvroConsumer(brukernotifkasjonFerdigTopic);
        sendOppgaveCron.sendBrukernotifikasjoner();
        ConsumerRecord<NokkelInput, OppgaveInput> singleRecord = getSingleRecord(avroAvroConsumer, oppgaveTopic, 5000);

        NokkelInput key = singleRecord.key();
        assertEquals(fnr.get(), key.getFodselsnummer());
        assertEquals(appname, key.getAppnavn());
        assertEquals(namespace, key.getNamespace());
        //TODO assert aktivitetdto
        return singleRecord;
    }


    public ConsumerRecord<NokkelInput, DoneInput> stoppet(Person.Fnr fnr, NokkelInput eventNokkel) {
        Consumer<NokkelInput, DoneInput> avroAvroConsumer = testService.createAvroAvroConsumer(brukernotifkasjonFerdigTopic);
        //Trigger scheduld jobb manuelt da schedule er disabled i test.
        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner();
        ConsumerRecord<NokkelInput, DoneInput> singleRecord = getSingleRecord(avroAvroConsumer, brukernotifkasjonFerdigTopic, 5000);
        NokkelInput key = singleRecord.key();
        assertEquals(fnr.get(), key.getFodselsnummer());
        assertEquals(eventNokkel.getEventId(), key.getEventId());
        assertEquals(appname, key.getAppnavn());
        assertEquals(namespace, key.getNamespace());

        return singleRecord;
    }
}
