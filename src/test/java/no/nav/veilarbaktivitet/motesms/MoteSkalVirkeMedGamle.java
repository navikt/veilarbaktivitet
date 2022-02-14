package no.nav.veilarbaktivitet.motesms;


import no.nav.brukernotifikasjon.schemas.Beskjed;
import no.nav.brukernotifikasjon.schemas.Done;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.KanalDTO;
import no.nav.veilarbaktivitet.brukernotifikasjon.oppgave.SendOppgaveCron;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import no.nav.veilarbaktivitet.motesms.gammel.MoteSMSMqService;
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.kafka.core.ConsumerFactory;

import java.time.ZonedDateTime;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

public class MoteSkalVirkeMedGamle extends SpringBootTestBase {
    @BeforeClass
    public static void settup() {
        System.setProperty("AKTIVITETSPLAN_URL", "aktivitesplan_url");
    }

    @Autowired
    MoteSMSService moteSMSService;

    Consumer<Nokkel, Beskjed> beskjedConsumer;

    Consumer<Nokkel, Done> doneConsumer;

    @Autowired
    MoteSMSMqService mqService;


    @Autowired
    ConsumerFactory<SpecificRecordBase, SpecificRecordBase> avroAvroConsumerFactory;

    @Value("${topic.ut.brukernotifikasjon.beskjed}")
    String beskjedTopic;

    @Value("${topic.ut.brukernotifikasjon.done}")
    String doneTopic;


    @Autowired
    SendOppgaveCron sendOppgaveCron;

    @LocalServerPort
    protected int port;

    @Before
    public void setUp() {
        DbTestUtils.cleanupTestDb(jdbcTemplate);

        beskjedConsumer = kafkaTestService.createAvroAvroConsumer(beskjedTopic);
        doneConsumer = kafkaTestService.createAvroAvroConsumer(doneTopic);
    }

    @Test
    public void skalVirkeMedGammelServicemeldingMedBytteKanal() {
        MockBruker happyBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(happyBruker);
        AktivitetDTO aktivitetDTO = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE);
        ZonedDateTime startTid = ZonedDateTime.now().plusHours(2);
        aktivitetDTO.setFraDato(new Date(startTid.toInstant().toEpochMilli()));
        aktivitetDTO.setKanal(KanalDTO.OPPMOTE);
        AktivitetDTO mote = aktivitetTestService.opprettAktivitet(port, happyBruker, veileder, aktivitetDTO);

        mqService.sendServicemeldingerForNesteDogn();

        moteSMSService.stopMoteSms();
        moteSMSService.sendMoteSms();
        sendOppgaveCron.sendBrukernotifikasjoner();
        assertTrue("skal ikke sende på nytt med samme tid og kanal",kafkaTestService.harKonsumertAlleMeldinger(beskjedTopic, beskjedConsumer));


        AktivitetDTO aktivitetDTO1 = aktivitetTestService.oppdatterAktivitet(port, happyBruker, veileder, mote.setKanal(KanalDTO.INTERNETT));
        moteSMSService.stopMoteSms();
        moteSMSService.sendMoteSms();
        sendOppgaveCron.sendBrukernotifikasjoner();

        final ConsumerRecord<Nokkel, Beskjed> oppgaveRecord = getSingleRecord(beskjedConsumer, beskjedTopic, 5000);
        Beskjed value = oppgaveRecord.value();


        MoteNotifikasjon expected = new MoteNotifikasjon(0L, 0L, happyBruker.getAktorIdAsAktorId(), KanalDTO.INTERNETT, startTid);
        assertEquals(happyBruker.getFnr(), value.getFodselsnummer());
        assertTrue(value.getEksternVarsling());
        assertEquals(expected.getSmsTekst(), value.getSmsVarslingstekst());
        assertEquals(expected.getDitNavTekst(), value.getTekst());
        assertEquals(expected.getEpostTitel(), value.getEpostVarslingstittel());
        assertEquals(expected.getEpostBody(), value.getEpostVarslingstekst());
        assertTrue(value.getLink().contains(mote.getId())); //TODO burde lage en test metode for aktivitets linker
    }

    @Test
    public void skalVirkeMedGammelServicemeldingMedBytteTid() {
        MockBruker happyBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(happyBruker);
        AktivitetDTO aktivitetDTO = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE);
        ZonedDateTime startTid = ZonedDateTime.now().plusHours(2);
        aktivitetDTO.setFraDato(new Date(startTid.toInstant().toEpochMilli()));
        aktivitetDTO.setKanal(KanalDTO.OPPMOTE);
        AktivitetDTO mote = aktivitetTestService.opprettAktivitet(port, happyBruker, veileder, aktivitetDTO);

        mqService.sendServicemeldingerForNesteDogn();

        moteSMSService.stopMoteSms();
        moteSMSService.sendMoteSms();
        sendOppgaveCron.sendBrukernotifikasjoner();
        assertTrue("skal ikke sende på nytt med samme tid og kanal",kafkaTestService.harKonsumertAlleMeldinger(beskjedTopic, beskjedConsumer));

        ZonedDateTime nyStartTid = startTid.plusHours(1);

        AktivitetDTO aktivitetDTO1 = aktivitetTestService.oppdatterAktivitet(port, happyBruker, veileder, mote.setFraDato(new Date(nyStartTid.toInstant().toEpochMilli())));
        moteSMSService.stopMoteSms();
        moteSMSService.sendMoteSms();
        sendOppgaveCron.sendBrukernotifikasjoner();

        final ConsumerRecord<Nokkel, Beskjed> oppgaveRecord = getSingleRecord(beskjedConsumer, beskjedTopic, 5000);
        Beskjed value = oppgaveRecord.value();


        MoteNotifikasjon expected = new MoteNotifikasjon(0L, 0L, happyBruker.getAktorIdAsAktorId(), KanalDTO.OPPMOTE, nyStartTid);
        assertEquals(happyBruker.getFnr(), value.getFodselsnummer());
        assertTrue(value.getEksternVarsling());
        assertEquals(expected.getSmsTekst(), value.getSmsVarslingstekst());
        assertEquals(expected.getDitNavTekst(), value.getTekst());
        assertEquals(expected.getEpostTitel(), value.getEpostVarslingstittel());
        assertEquals(expected.getEpostBody(), value.getEpostVarslingstekst());
        assertTrue(value.getLink().contains(mote.getId()));
    }

    @Test
    public void skalIkkeSendePaaNyttMedAnnenOppdatering() {
        MockBruker happyBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(happyBruker);
        AktivitetDTO aktivitetDTO = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE);
        ZonedDateTime startTid = ZonedDateTime.now().plusHours(2);
        aktivitetDTO.setFraDato(new Date(startTid.toInstant().toEpochMilli()));
        aktivitetDTO.setKanal(KanalDTO.OPPMOTE);
        AktivitetDTO mote = aktivitetTestService.opprettAktivitet(port, happyBruker, veileder, aktivitetDTO);

        mqService.sendServicemeldingerForNesteDogn();

        moteSMSService.stopMoteSms();
        moteSMSService.sendMoteSms();
        sendOppgaveCron.sendBrukernotifikasjoner();
        assertTrue("skal ikke sende på nytt med samme tid og kanal",kafkaTestService.harKonsumertAlleMeldinger(beskjedTopic, beskjedConsumer));


        AktivitetDTO aktivitetDTO1 = aktivitetTestService.oppdatterAktivitet(port, happyBruker, veileder, mote.setTittel("ny tittel for test"));
        moteSMSService.stopMoteSms();
        moteSMSService.sendMoteSms();
        sendOppgaveCron.sendBrukernotifikasjoner();
        assertTrue("skal ikke sende på nytt med oppdatert Titel",kafkaTestService.harKonsumertAlleMeldinger(beskjedTopic, beskjedConsumer));
    }
}
