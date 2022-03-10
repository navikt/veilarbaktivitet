package no.nav.veilarbaktivitet.brukernotifikasjon;

import lombok.SneakyThrows;
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput;
import no.nav.brukernotifikasjon.schemas.input.DoneInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.brukernotifikasjon.avslutt.AvsluttBrukernotifikasjonCron;
import no.nav.veilarbaktivitet.brukernotifikasjon.oppgave.SendOppgaveCron;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaAvroTemplate;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.util.KafkaTestService;
import org.apache.avro.specific.SpecificRecord;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import static no.nav.veilarbaktivitet.brukernotifikasjon.kvitering.EksternVarslingKvitteringConsumer.FEILET;
import static no.nav.veilarbaktivitet.brukernotifikasjon.kvitering.EksternVarslingKvitteringConsumer.FERDIGSTILT;
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

    @Value("${topic.inn.eksternVarselKvittering}")
    String kviteringsToppic;

    @Autowired
    KafkaAvroTemplate<DoknotifikasjonStatus> kviteringsTopic;

    @Value("${app.env.appname}")
    private String appname;
    @Value("${app.env.namespace}")
    private String namespace;

    @Autowired
    private AvsluttBrukernotifikasjonCron avsluttBrukernotifikasjonCron;

    @Autowired
    SendOppgaveCron sendOppgaveCron;

    public ConsumerRecord<NokkelInput, OppgaveInput> oppgaveSendt(Person.Fnr fnr, AktivitetDTO aktivitetDTO) {
        Consumer<NokkelInput, OppgaveInput> avroAvroConsumer = testService.createAvroAvroConsumer(oppgaveTopic);
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


    public void sendEksternVarseltOk(ConsumerRecord record) {
        sendVarsel(record, FERDIGSTILT);
    }

    public void sendEksternVarseletFeilet(ConsumerRecord record) {
        sendVarsel(record, FEILET);
    }

    @SneakyThrows
    public void sendVarsel(ConsumerRecord<NokkelInput, SpecificRecord> record, String status) {
        String kviteringsId = getKviteringsId(record);
        DoknotifikasjonStatus doknot = doknotifikasjonStatus(kviteringsId, status);
        SendResult<String, DoknotifikasjonStatus> result = kviteringsTopic.send(kviteringsToppic, kviteringsId, doknot).get();
        testService.assertErKonsumertAiven(kviteringsToppic, result.getRecordMetadata().offset(), 5);
    }


    private DoknotifikasjonStatus doknotifikasjonStatus(String bestillingsId, String status) {
        return DoknotifikasjonStatus
                .newBuilder()
                .setStatus(status)
                .setBestillingsId(bestillingsId)
                .setBestillerId("veilarbaktivitet")
                .setMelding("her er en melding")
                .setDistribusjonId(1L)
                .build();
    }

    private String getKviteringsId(ConsumerRecord<NokkelInput, SpecificRecord> record) {
        var valueClass = record.value().getClass();
        if (BeskjedInput.class.equals(valueClass)) {
            return "B-" + appname + "-" + record.key().getEventId();
        }
        if(OppgaveInput.class.equals(valueClass)) {
            return "O-" + appname + "-" + record.key().getEventId();
        }
        if(DoneInput.class.equals(valueClass)) {
            throw new IllegalArgumentException("Ikke nokk informasjon i denne til og finne kviterings id");
        }
        if(DoneInput.class.getPackage().equals(valueClass.getPackage())) {
            throw new NotImplementedException("ukjent klasse fra brukernotifikasjoner");
        }
        throw new IllegalArgumentException("Kommer denne klassen fra brukernotifikasjoner?");
    }
}
