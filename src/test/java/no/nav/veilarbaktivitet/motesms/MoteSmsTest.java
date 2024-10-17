package no.nav.veilarbaktivitet.motesms;


import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.KanalDTO;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAsserts;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAssertsConfig;
import no.nav.veilarbaktivitet.brukernotifikasjon.OpprettVarselDto;
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.SendBrukernotifikasjonCron;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoteSmsTest extends SpringBootTestBase {

    @Autowired
    MoteSMSService moteSMSService;

    @Autowired
    BrukernotifikasjonAssertsConfig brukernotifikasjonAssertsConfig;
    BrukernotifikasjonAsserts brukernotifikasjonAsserts;

    @Autowired
    SendBrukernotifikasjonCron sendBrukernotifikasjonCron;

    @Autowired
    AktivitetService aktivitetService;

    @LocalServerPort
    protected int port;

    @BeforeEach
    void setUp() {
        DbTestUtils.cleanupTestDb(jdbcTemplate);
        brukernotifikasjonAsserts = new BrukernotifikasjonAsserts(brukernotifikasjonAssertsConfig);
    }

    @Test
    void skalSendeServiceMelding() {
        MockBruker happyBruker = navMockService.createHappyBruker(BrukerOptions.happyBruker());
        MockVeileder veileder = MockNavService.createVeileder(happyBruker);
        AktivitetDTO aktivitetDTO = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE);
        ZonedDateTime startTid = ZonedDateTime.now().plusHours(2);
        aktivitetDTO.setFraDato(new Date(startTid.toInstant().toEpochMilli()));
        aktivitetDTO.setKanal(KanalDTO.OPPMOTE);
        AktivitetDTO mote = aktivitetTestService.opprettAktivitet(happyBruker, veileder, aktivitetDTO);

        moteSmsCronjobber();
        OpprettVarselDto orginalMelding = assertForventetMeldingSendt("Varsel skal ha innhold", happyBruker, KanalDTO.OPPMOTE, startTid, mote);
        brukernotifikasjonAsserts.assertSkalIkkeHaProdusertFlereMeldinger();

        moteSmsCronjobber();
        brukernotifikasjonAsserts.assertSkalIkkeHaProdusertFlereMeldinger();

        AktivitetDTO nyKanal = aktivitetTestService.oppdaterAktivitetOk(happyBruker, veileder, mote.setKanal(KanalDTO.TELEFON));
        moteSmsCronjobber();
        harAvsluttetVarsel(orginalMelding);
        OpprettVarselDto ny_kanal_varsel = assertForventetMeldingSendt("Varsel skal ha nyKanal", happyBruker, KanalDTO.TELEFON, startTid, mote);


        ZonedDateTime ny_startTid = startTid.plusHours(2);
        AktivitetDTO nyTid = aktivitetTestService.oppdaterAktivitetOk(happyBruker, veileder, nyKanal.setFraDato(new Date(ny_startTid.toInstant().toEpochMilli())));

        moteSmsCronjobber();
        harAvsluttetVarsel(ny_kanal_varsel);
        assertForventetMeldingSendt("Varsel skal ha tid", happyBruker, KanalDTO.TELEFON, ny_startTid, mote);

        aktivitetTestService.oppdaterAktivitetOk(happyBruker, veileder, nyTid.setTittel("ny test tittel skal ikke oppdatere varsel"));
        moteSmsCronjobber();
        brukernotifikasjonAsserts.assertSkalIkkeHaProdusertFlereMeldinger(); //"skal ikke sende på nytt for andre oppdateringer"

    }

    @Test
    void skalSendeForAlleMoteTyper() {
        MockBruker happyBruker = navMockService.createHappyBruker(BrukerOptions.happyBruker());
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
    void bareSendeForMote() {
        MockBruker happyBruker = navMockService.createHappyBruker(BrukerOptions.happyBruker());
        MockVeileder veileder = MockNavService.createVeileder(happyBruker);
        for (AktivitetTypeDTO type :
                AktivitetTypeDTO.values()) {
            if (type == AktivitetTypeDTO.STILLING_FRA_NAV) {
                aktivitetTestService.opprettStillingFraNav(happyBruker);
            } else if (type == AktivitetTypeDTO.EKSTERNAKTIVITET) {
                // TODO aktivitetTestService.opprettEksternAktivitet(happyBruker)
            } else {
                AktivitetDTO aktivitet = AktivitetDtoTestBuilder.nyAktivitet(type);
                aktivitet.setFraDato(new Date(ZonedDateTime.now().plusHours(4).toInstant().toEpochMilli()));
                aktivitetTestService.opprettAktivitet(happyBruker, veileder, aktivitet);
            }
        }

        moteSmsCronjobber();
        // Blir først sendt en oppgave på stilling-fra-nav
//        brukernotifikasjonAsserts.assertOppgaveSendt(happyBruker.getFnrAsFnr());
        // Blir sendt en beskjed på mote
        brukernotifikasjonAsserts.assertBeskjedSendt(happyBruker.getFnrAsFnr());
        brukernotifikasjonAsserts.assertSkalIkkeHaProdusertFlereMeldinger();
    }

    @Test
    void skalFjereneGamleMoter() {
        MockBruker happyBruker = navMockService.createHappyBruker(BrukerOptions.happyBruker());
        MockVeileder veileder = MockNavService.createVeileder(happyBruker);
        AktivitetDTO aktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE);
        ZonedDateTime startTid = ZonedDateTime.now().minusDays(10);
        aktivitet.setFraDato(new Date(startTid.toInstant().toEpochMilli()));
        AktivitetDTO response = aktivitetTestService.opprettAktivitet(happyBruker, veileder, aktivitet);

        moteSMSService.sendServicemeldinger(Duration.ofDays(-15), Duration.ofDays(0));
        sendBrukernotifikasjonCron.sendBrukernotifikasjoner();
        OpprettVarselDto varsel = assertForventetMeldingSendt("skall ha opprettet gamelt varsel", happyBruker, KanalDTO.OPPMOTE, startTid, response);

        moteSmsCronjobber();

        harAvsluttetVarsel(varsel);
        brukernotifikasjonAsserts.assertSkalIkkeHaProdusertFlereMeldinger();
    }

    @Test
    void skalIkkeOppreteVarsleHistorisk() {
        MockBruker happyBruker = navMockService.createHappyBruker(BrukerOptions.happyBruker());
        MockVeileder veileder = MockNavService.createVeileder(happyBruker);
        AktivitetDTO aktivitetDTO = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE);
        ZonedDateTime startTid = ZonedDateTime.now().plusHours(2);
        aktivitetDTO.setFraDato(new Date(startTid.toInstant().toEpochMilli()));
        aktivitetDTO.setKanal(KanalDTO.OPPMOTE);
        aktivitetTestService.opprettAktivitet(happyBruker, veileder, aktivitetDTO);
        aktivitetService.settAktiviteterTilHistoriske(happyBruker.getOppfolgingsperiodeId(), ZonedDateTime.now());


        moteSmsCronjobber();
        int antall = jdbcTemplate.queryForObject("Select count(*) from GJELDENDE_MOTE_SMS", Integer.class);
        assertEquals(0, antall);
        brukernotifikasjonAsserts.assertSkalIkkeHaProdusertFlereMeldinger();

    }

    @Test
    void skalIkkeOppreteVarsleFulfort() {
        MockBruker happyBruker = navMockService.createHappyBruker(BrukerOptions.happyBruker());
        MockVeileder veileder = MockNavService.createVeileder(happyBruker);
        AktivitetDTO aktivitetDTO = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE);
        ZonedDateTime startTid = ZonedDateTime.now().plusHours(2);
        aktivitetDTO.setFraDato(new Date(startTid.toInstant().toEpochMilli()));
        AktivitetDTO mote = aktivitetTestService.opprettAktivitet(happyBruker, veileder, aktivitetDTO);
        aktivitetTestService.oppdaterAktivitetStatus(happyBruker, veileder, mote, AktivitetStatus.FULLFORT);

        moteSmsCronjobber();
        int antall = jdbcTemplate.queryForObject("Select count(*) from GJELDENDE_MOTE_SMS", Integer.class);
        assertEquals(0, antall);
        brukernotifikasjonAsserts.assertSkalIkkeHaProdusertFlereMeldinger();

    }

    @Test
    void skalIkkeOppreteVarsleAvbrutt() {
        MockBruker happyBruker = navMockService.createHappyBruker(BrukerOptions.happyBruker());
        MockVeileder veileder = MockNavService.createVeileder(happyBruker);
        AktivitetDTO aktivitetDTO = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE);
        ZonedDateTime startTid = ZonedDateTime.now().plusHours(2);
        aktivitetDTO.setFraDato(new Date(startTid.toInstant().toEpochMilli()));
        AktivitetDTO mote = aktivitetTestService.opprettAktivitet(happyBruker, veileder, aktivitetDTO);
        aktivitetTestService.oppdaterAktivitetStatus(happyBruker, veileder, mote, AktivitetStatus.AVBRUTT);

        moteSmsCronjobber();
        int antall = jdbcTemplate.queryForObject("Select count(*) from GJELDENDE_MOTE_SMS", Integer.class);
        assertEquals(0, antall);
        brukernotifikasjonAsserts.assertSkalIkkeHaProdusertFlereMeldinger();
    }


    private void harAvsluttetVarsel(OpprettVarselDto varsel) {
        brukernotifikasjonAsserts.assertInaktivertMeldingErSendt(varsel.getVarselId());
    }

    private void moteSmsCronjobber() {
        moteSMSService.stopMoteSms();
        moteSMSService.sendMoteSms();
    }

    private OpprettVarselDto assertForventetMeldingSendt(String melding, MockBruker happyBruker, KanalDTO oppmote, ZonedDateTime startTid, AktivitetDTO mote) {
        var oppgave = brukernotifikasjonAsserts.assertBeskjedSendt(happyBruker.getFnrAsFnr());

        MoteNotifikasjon expected = new MoteNotifikasjon(0L, 0L, happyBruker.getAktorIdAsAktorId(), oppmote, startTid);
        assertEquals(happyBruker.getFnr(), oppgave.getIdent(), melding + " fnr");
        assertTrue(oppgave.getEksternVarsling() != null, melding + " eksternvarsling");
        assertEquals(expected.getSmsTekst(), oppgave.getEksternVarsling().getSmsVarslingstekst(), melding + " sms tekst");
        assertEquals(expected.getDitNavTekst(), oppgave.getTekster().getFirst().getTekst(), melding + " ditnav tekst");
        assertEquals(expected.getEpostTitel(), oppgave.getEksternVarsling().getEpostVarslingstittel(), melding + " epost tittel tekst");
        assertEquals(expected.getEpostBody(), oppgave.getEksternVarsling().getEpostVarslingstekst(), melding + " epost body tekst");
        assertTrue(oppgave.getLink().contains(mote.getId()), melding + " mote link tekst"); //TODO burde lage en test metode for aktivitets linker
        return oppgave;
    }
}
