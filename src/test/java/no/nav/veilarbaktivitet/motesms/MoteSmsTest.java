package no.nav.veilarbaktivitet.motesms;


import no.nav.brukernotifikasjon.schemas.input.BeskjedInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.KanalDTO;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAsserts;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAssertsConfig;
import no.nav.veilarbaktivitet.brukernotifikasjon.avslutt.AvsluttBrukernotifikasjonCron;
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.SendBrukernotifikasjonCron;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.kafka.core.ConsumerFactory;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class MoteSmsTest extends SpringBootTestBase {

    @Autowired
    MoteSMSService moteSMSService;

    @Autowired
    BrukernotifikasjonAssertsConfig brukernotifikasjonAssertsConfig;
    BrukernotifikasjonAsserts brukernotifikasjonAsserts;

    @Autowired
    ConsumerFactory<SpecificRecordBase, SpecificRecordBase> avroAvroConsumerFactory;

    @Value("${topic.ut.brukernotifikasjon.beskjed}")
    String beskjedTopic;

    @Value("${topic.ut.brukernotifikasjon.done}")
    String doneTopic;


    @Autowired
    SendBrukernotifikasjonCron sendBrukernotifikasjonCron;

    @Autowired
    AvsluttBrukernotifikasjonCron avsluttBrukernotifikasjonCron;

    @Autowired
    AktivitetService aktivitetService;

    @LocalServerPort
    protected int port;

    @Before
    public void setUp() {
        DbTestUtils.cleanupTestDb(jdbcTemplate);
        brukernotifikasjonAsserts = new BrukernotifikasjonAsserts(brukernotifikasjonAssertsConfig);
    }

    @Test
    public void skalSendeServiceMelding() {
        MockBruker happyBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(happyBruker);
        AktivitetDTO aktivitetDTO = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE);
        ZonedDateTime startTid = ZonedDateTime.now().plusHours(2);
        aktivitetDTO.setFraDato(new Date(startTid.toInstant().toEpochMilli()));
        aktivitetDTO.setKanal(KanalDTO.OPPMOTE);
        AktivitetDTO mote = aktivitetTestService.opprettAktivitet(happyBruker, veileder, aktivitetDTO);

        moteSmsCronjobber();
        ConsumerRecord<NokkelInput, BeskjedInput> orginalMelding = assertForventetMeldingSendt("Varsel skal ha innhold", happyBruker, KanalDTO.OPPMOTE, startTid, mote);
        brukernotifikasjonAsserts.assertSkalIkkeHaProdusertFlereMeldinger();

        moteSmsCronjobber();
        brukernotifikasjonAsserts.assertSkalIkkeHaProdusertFlereMeldinger();

        AktivitetDTO nyKanal = aktivitetTestService.oppdatterAktivitet(port, happyBruker, veileder, mote.setKanal(KanalDTO.TELEFON));
        moteSmsCronjobber();
        harAvsluttetVarsel(orginalMelding);
        ConsumerRecord<NokkelInput, BeskjedInput> ny_kanal_varsel = assertForventetMeldingSendt("Varsel skal ha nyKanal", happyBruker, KanalDTO.TELEFON, startTid, mote);


        ZonedDateTime ny_startTid = startTid.plusHours(2);
        AktivitetDTO nyTid = aktivitetTestService.oppdatterAktivitet(port, happyBruker, veileder, nyKanal.setFraDato(new Date(ny_startTid.toInstant().toEpochMilli())));

        moteSmsCronjobber();
        harAvsluttetVarsel(ny_kanal_varsel);
        assertForventetMeldingSendt("Varsel skal ha tid", happyBruker, KanalDTO.TELEFON, ny_startTid, mote);

        aktivitetTestService.oppdatterAktivitet(port, happyBruker, veileder, nyTid.setTittel("ny test tittel skal ikke oppdatere varsel"));
        moteSmsCronjobber();
        brukernotifikasjonAsserts.assertSkalIkkeHaProdusertFlereMeldinger(); //"skal ikke sende på nytt for andre oppdateringer"

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
            AktivitetDTO response = aktivitetTestService.opprettAktivitet(happyBruker, veileder, aktivitet);

            moteSmsCronjobber();
            assertForventetMeldingSendt(kanal.name() + "skal ha riktig melding", happyBruker, kanal, fraDato, response);
            brukernotifikasjonAsserts.assertSkalIkkeHaProdusertFlereMeldinger();
        }
    }

    @Test
    public void bareSendeForMote() {
        MockBruker happyBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(happyBruker);
        for (AktivitetTypeDTO type :
                AktivitetTypeDTO.values()) {
            if (type == AktivitetTypeDTO.STILLING_FRA_NAV) {
                aktivitetTestService.opprettStillingFraNav(happyBruker);
            } else {
                AktivitetDTO aktivitet = AktivitetDtoTestBuilder.nyAktivitet(type);
                aktivitet.setFraDato(new Date(ZonedDateTime.now().plusHours(4).toInstant().toEpochMilli()));
                aktivitetTestService.opprettAktivitet(happyBruker, veileder, aktivitet);
            }
        }

        moteSmsCronjobber();
        brukernotifikasjonAsserts.assertBeskjedSendt(happyBruker.getFnrAsFnr());
        brukernotifikasjonAsserts.assertSkalIkkeHaProdusertFlereMeldinger();
    }

    @Test
    public void skalFjereneGamleMoter() {
        MockBruker happyBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(happyBruker);
        AktivitetDTO aktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE);
        ZonedDateTime startTid = ZonedDateTime.now().minusDays(10);
        aktivitet.setFraDato(new Date(startTid.toInstant().toEpochMilli()));
        AktivitetDTO response = aktivitetTestService.opprettAktivitet(happyBruker, veileder, aktivitet);

        moteSMSService.sendServicemeldinger(Duration.ofDays(-15), Duration.ofDays(0));
        sendBrukernotifikasjonCron.sendBrukernotifikasjoner();
        ConsumerRecord<NokkelInput, BeskjedInput> varsel = assertForventetMeldingSendt("skall ha opprettet gamelt varsel", happyBruker, KanalDTO.OPPMOTE, startTid, response);

        moteSmsCronjobber();

        harAvsluttetVarsel(varsel);
        brukernotifikasjonAsserts.assertSkalIkkeHaProdusertFlereMeldinger();
    }

    @Test
    public void skalIkkeOppreteVarsleHistorisk() {
        MockBruker happyBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(happyBruker);
        AktivitetDTO aktivitetDTO = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE);
        ZonedDateTime startTid = ZonedDateTime.now().plusHours(2);
        aktivitetDTO.setFraDato(new Date(startTid.toInstant().toEpochMilli()));
        aktivitetDTO.setKanal(KanalDTO.OPPMOTE);
        AktivitetDTO mote = aktivitetTestService.opprettAktivitet(happyBruker, veileder, aktivitetDTO);
        aktivitetService.settAktiviteterTilHistoriske(happyBruker.getOppfolgingsperiode(), ZonedDateTime.now());


        moteSmsCronjobber();
        int antall = jdbcTemplate.queryForObject("Select count(*) from GJELDENDE_MOTE_SMS", Integer.class);
        assertEquals(0, antall);
        brukernotifikasjonAsserts.assertSkalIkkeHaProdusertFlereMeldinger();

    }

    @Test
    public void skalIkkeOppreteVarsleFulfort() {
        MockBruker happyBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(happyBruker);
        AktivitetDTO aktivitetDTO = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE);
        ZonedDateTime startTid = ZonedDateTime.now().plusHours(2);
        aktivitetDTO.setFraDato(new Date(startTid.toInstant().toEpochMilli()));
        AktivitetDTO mote = aktivitetTestService.opprettAktivitet(happyBruker, veileder, aktivitetDTO);
        aktivitetTestService.oppdatterAktivitetStatus(happyBruker, veileder, mote, AktivitetStatus.FULLFORT);

        moteSmsCronjobber();
        int antall = jdbcTemplate.queryForObject("Select count(*) from GJELDENDE_MOTE_SMS", Integer.class);
        assertEquals(0, antall);
        brukernotifikasjonAsserts.assertSkalIkkeHaProdusertFlereMeldinger();

    }

    @Test
    public void skalIkkeOppreteVarsleAvbrutt() {
        MockBruker happyBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(happyBruker);
        AktivitetDTO aktivitetDTO = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE);
        ZonedDateTime startTid = ZonedDateTime.now().plusHours(2);
        aktivitetDTO.setFraDato(new Date(startTid.toInstant().toEpochMilli()));
        AktivitetDTO mote = aktivitetTestService.opprettAktivitet(happyBruker, veileder, aktivitetDTO);
        aktivitetTestService.oppdatterAktivitetStatus(happyBruker, veileder, mote, AktivitetStatus.AVBRUTT);

        moteSmsCronjobber();
        int antall = jdbcTemplate.queryForObject("Select count(*) from GJELDENDE_MOTE_SMS", Integer.class);
        assertEquals(0, antall);
        brukernotifikasjonAsserts.assertSkalIkkeHaProdusertFlereMeldinger();
    }


    private void harAvsluttetVarsel(ConsumerRecord<NokkelInput, BeskjedInput> varsel) {
        brukernotifikasjonAsserts.assertDone(varsel.key());
    }

    private void moteSmsCronjobber() {
        moteSMSService.stopMoteSms();
        moteSMSService.sendMoteSms();
    }

    private ConsumerRecord<NokkelInput, BeskjedInput> assertForventetMeldingSendt(String melding, MockBruker happyBruker, KanalDTO oppmote, ZonedDateTime startTid, AktivitetDTO mote) {
        ConsumerRecord<NokkelInput, BeskjedInput> oppgaveRecord = brukernotifikasjonAsserts.assertBeskjedSendt(happyBruker.getFnrAsFnr(), mote);
        BeskjedInput value = oppgaveRecord.value();

        MoteNotifikasjon expected = new MoteNotifikasjon(0L, 0L, happyBruker.getAktorIdAsAktorId(), oppmote, startTid);
        assertEquals(melding + " fnr", happyBruker.getFnr(), oppgaveRecord.key().getFodselsnummer());
        assertTrue(melding + " eksternvarsling", value.getEksternVarsling());
        assertEquals(melding + " sms tekst", expected.getSmsTekst(), value.getSmsVarslingstekst());
        assertEquals(melding + " ditnav tekst", expected.getDitNavTekst(), value.getTekst());
        assertEquals(melding + " epost tittel tekst", expected.getEpostTitel(), value.getEpostVarslingstittel());
        assertEquals(melding + " epost body tekst", expected.getEpostBody(), value.getEpostVarslingstekst());
        assertTrue(melding + " mote link tekst", value.getLink().contains(mote.getId())); //TODO burde lage en test metode for aktivitets linker
        return oppgaveRecord;
    }
}
