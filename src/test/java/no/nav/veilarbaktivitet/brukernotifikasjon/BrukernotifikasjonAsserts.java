package no.nav.veilarbaktivitet.brukernotifikasjon;

import lombok.SneakyThrows;
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput;
import no.nav.brukernotifikasjon.schemas.input.DoneInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringAvroTemplate;
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.util.KafkaTestService;
import org.apache.avro.specific.SpecificRecord;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.SendResult;

import static no.nav.veilarbaktivitet.brukernotifikasjon.kvitering.EksternVarslingKvitteringConsumer.FEILET;
import static no.nav.veilarbaktivitet.brukernotifikasjon.kvitering.EksternVarslingKvitteringConsumer.FERDIGSTILT;
import static org.junit.Assert.assertEquals;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

public class BrukernotifikasjonAsserts {
    Consumer<NokkelInput, OppgaveInput> oppgaveConsumer;
    Consumer<NokkelInput, BeskjedInput> beskjedConsumer;
    Consumer<NokkelInput, DoneInput> doneInputConsumer;
    private KafkaStringAvroTemplate<DoknotifikasjonStatus> kviteringsProducer;
    BrukernotifikasjonAssertsConfig config;
    KafkaTestService kafkaTestService;

    public static MockBruker getBrukerSomIkkeKanVarsles() {
        return MockNavService.createBruker(BrukerOptions.happyBrukerBuilder().harBruktNivaa4(false).build());
    }

    public BrukernotifikasjonAsserts(BrukernotifikasjonAssertsConfig config) {
        oppgaveConsumer = config.createOppgaveConsumer();
        beskjedConsumer = config.createBeskjedConsumer();
        kviteringsProducer = config.getKviteringsProducer();
        doneInputConsumer = config.createDoneConsumer();
        kafkaTestService = config.getTestService();
        this.config = config;
    }

    public ConsumerRecord<NokkelInput, OppgaveInput> oppgaveSendt(Person.Fnr fnr, AktivitetDTO aktivitetDTO) {
        config.getSendOppgaveCron().sendBrukernotifikasjoner();
        ConsumerRecord<NokkelInput, OppgaveInput> singleRecord = getSingleRecord(oppgaveConsumer, config.getOppgaveTopic(), 10000);

        NokkelInput key = singleRecord.key();
        assertEquals(fnr.get(), key.getFodselsnummer());
        assertEquals(config.getAppname(), key.getAppnavn());
        assertEquals(config.getNamespace(), key.getNamespace());
        //TODO assert aktivitetdto
        return singleRecord;
    }

    public ConsumerRecord<NokkelInput, BeskjedInput> assertBeskjedSendt(Person.Fnr fnr, AktivitetDTO aktivitetDTO) {
        return assertBeskjedSendt(fnr);
        //TODO assert aktivitetdto
    }

    public ConsumerRecord<NokkelInput, BeskjedInput> assertBeskjedSendt(Person.Fnr fnr) {
        config.getSendOppgaveCron().sendBrukernotifikasjoner();
        ConsumerRecord<NokkelInput, BeskjedInput> singleRecord = getSingleRecord(beskjedConsumer, config.getBeskjedTopic(), 10000);

        NokkelInput key = singleRecord.key();
        assertEquals(fnr.get(), key.getFodselsnummer());
        assertEquals(config.getAppname(), key.getAppnavn());
        assertEquals(config.getNamespace(), key.getNamespace());

        return singleRecord;
    }


    public ConsumerRecord<NokkelInput, DoneInput> stoppet(NokkelInput eventNokkel) {
        //Trigger scheduld jobb manuelt da schedule er disabled i test.
        config.getAvsluttBrukernotifikasjonCron().avsluttBrukernotifikasjoner();
        ConsumerRecord<NokkelInput, DoneInput> singleRecord = getSingleRecord(doneInputConsumer, config.getBrukernotifkasjonFerdigTopic(), 10000);
        NokkelInput key = singleRecord.key();
        assertEquals(eventNokkel.getFodselsnummer(), key.getFodselsnummer());
        assertEquals(eventNokkel.getEventId(), key.getEventId());
        assertEquals(config.getAppname(), key.getAppnavn());
        assertEquals(config.getNamespace(), key.getNamespace());

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
        SendResult<String, DoknotifikasjonStatus> result = kviteringsProducer.send(config.getKviteringsToppic(), kviteringsId, doknot).get();
        kafkaTestService.assertErKonsumertAiven(config.getKviteringsToppic(), result.getRecordMetadata().offset(), 5);
    }

    public void assertSkalIkkeHaProdusertFlereMeldinger() {
        config.getAvsluttBrukernotifikasjonCron().avsluttBrukernotifikasjoner();
        config.getSendOppgaveCron().sendBrukernotifikasjoner();

        kafkaTestService.harKonsumertAlleMeldinger(config.getOppgaveTopic(), oppgaveConsumer);
        kafkaTestService.harKonsumertAlleMeldinger(config.getBrukernotifkasjonFerdigTopic(), doneInputConsumer);
        kafkaTestService.harKonsumertAlleMeldinger(config.getBeskjedTopic(), beskjedConsumer);
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
            return "B-" + config.getAppname() + "-" + record.key().getEventId();
        }
        if(OppgaveInput.class.equals(valueClass)) {
            return "O-" + config.getAppname() + "-" + record.key().getEventId();
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
