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
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Before;
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


public class MoteSmsTest extends SpringBootTestBase {

    @Autowired
    MoteSMSService moteSMSService;

    Consumer<Nokkel, Beskjed> beskjedConsumer;

    Consumer<Nokkel, Done> doneConsumer;


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
    public void skalSendeServiceMelding() {
        MockBruker happyBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(happyBruker);
        AktivitetDTO aktivitetDTO = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE);
        ZonedDateTime startTid = ZonedDateTime.now().plusHours(2);
        aktivitetDTO.setFraDato(new Date(startTid.toInstant().toEpochMilli()));
        aktivitetDTO.setKanal(KanalDTO.OPPMOTE);
        AktivitetDTO mote = aktivitetTestService.opprettAktivitet(port, happyBruker, veileder, aktivitetDTO);

        moteSMSService.stopMoteSms();
        moteSMSService.sendMoteSms();
        sendOppgaveCron.sendBrukernotifikasjoner();
        assertForventetMeldingSendt("Varsel skal ha innhold", happyBruker, KanalDTO.OPPMOTE, startTid, mote);

        moteSMSService.stopMoteSms();
        moteSMSService.sendMoteSms();
        sendOppgaveCron.sendBrukernotifikasjoner();
        assertTrue("skal ikke sende på nytt",kafkaTestService.harKonsumertAlleMeldinger(beskjedTopic, beskjedConsumer));

        AktivitetDTO nyKanal = aktivitetTestService.oppdatterAktivitet(port, happyBruker, veileder, mote.setKanal(KanalDTO.TELEFON));

        moteSMSService.stopMoteSms();
        moteSMSService.sendMoteSms();
        sendOppgaveCron.sendBrukernotifikasjoner();
        assertForventetMeldingSendt("Varsel skal ha nyKanal", happyBruker, KanalDTO.TELEFON, startTid, mote);


        ZonedDateTime ny_startTid = startTid.plusHours(2);
        AktivitetDTO nyTid = aktivitetTestService.oppdatterAktivitet(port, happyBruker, veileder, nyKanal.setFraDato(new Date(ny_startTid.toInstant().toEpochMilli())));

        moteSMSService.stopMoteSms();
        moteSMSService.sendMoteSms();
        sendOppgaveCron.sendBrukernotifikasjoner();
        assertForventetMeldingSendt("Varsel skal ha tid", happyBruker, KanalDTO.TELEFON, ny_startTid, mote);


        aktivitetTestService.oppdatterAktivitet(port, happyBruker, veileder, nyTid.setTittel("ny test tittel skal ikke oppdatere varsel"));
        moteSMSService.stopMoteSms();
        moteSMSService.sendMoteSms();
        sendOppgaveCron.sendBrukernotifikasjoner();
        assertTrue("skal ikke sende på nytt for andre oppdateringer",kafkaTestService.harKonsumertAlleMeldinger(beskjedTopic, beskjedConsumer));

    }

    private ConsumerRecord<Nokkel, Beskjed> assertForventetMeldingSendt(String melding, MockBruker happyBruker, KanalDTO oppmote, ZonedDateTime startTid,AktivitetDTO mote) {
        final ConsumerRecord<Nokkel, Beskjed> oppgaveRecord = getSingleRecord(beskjedConsumer, beskjedTopic, 5000);
        Beskjed value = oppgaveRecord.value();

        MoteNotifikasjon expected = new MoteNotifikasjon(0L, 0L, happyBruker.getAktorIdAsAktorId(), oppmote, startTid);
        assertEquals(melding, happyBruker.getFnr(), value.getFodselsnummer());
        assertTrue(melding, value.getEksternVarsling());
        assertEquals(melding, expected.getSmsTekst(), value.getSmsVarslingstekst());
        assertEquals(melding, expected.getDitNavTekst(), value.getTekst());
        assertEquals(melding, expected.getEpostTitel(), value.getEpostVarslingstittel());
        assertEquals(melding, expected.getEpostBody(), value.getEpostVarslingstekst());
        assertTrue(melding, value.getLink().contains(mote.getId())); //TODO burde lage en test metode for aktivitets linker
        return oppgaveRecord;
    }

    @Test
    public void skalSendeForAlleMoteTyper() {
        MockBruker happyBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(happyBruker);
        AktivitetDTO aktivitetDTO = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE);
        ZonedDateTime fraDato = ZonedDateTime.now().plusHours(4);
        aktivitetDTO.setFraDato(new Date(fraDato.toInstant().toEpochMilli()));

        for (KanalDTO kanal : KanalDTO.values()) {
            AktivitetDTO aktivitet = aktivitetDTO.toBuilder().kanal(kanal).build();
            AktivitetDTO response = aktivitetTestService.opprettAktivitet(port, happyBruker, veileder, aktivitet);

            moteSMSService.sendMoteSms();
            sendOppgaveCron.sendBrukernotifikasjoner();
            assertForventetMeldingSendt(kanal.name() + "skal ha riktig melding", happyBruker, kanal, fraDato,response);
            assertTrue(kafkaTestService.harKonsumertAlleMeldinger(beskjedTopic, beskjedConsumer));
        }
    }

    @Test
    public void bareSendeForMote() {
        MockBruker happyBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(happyBruker);
        for (AktivitetTypeDTO type :
                AktivitetTypeDTO.values()) {
            if(type == AktivitetTypeDTO.STILLING_FRA_NAV) {
                aktivitetTestService.opprettStillingFraNav(happyBruker, port);
            }

            AktivitetDTO aktivitet = AktivitetDtoTestBuilder.nyAktivitet(type);
            aktivitet.setFraDato(new Date(ZonedDateTime.now().plusHours(4).toInstant().toEpochMilli()));
            aktivitetTestService.opprettAktivitet(port, happyBruker, veileder, aktivitet);

        }
        moteSMSService.sendMoteSms();
        sendOppgaveCron.sendBrukernotifikasjoner();
        getSingleRecord(beskjedConsumer, beskjedTopic, 5000);
        assertTrue("skal bare ha opprettet sms for møtet", kafkaTestService.harKonsumertAlleMeldinger(beskjedTopic, beskjedConsumer));
    }
}
