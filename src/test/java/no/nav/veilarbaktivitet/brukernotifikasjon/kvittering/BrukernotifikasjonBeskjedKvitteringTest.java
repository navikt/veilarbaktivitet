package no.nav.veilarbaktivitet.brukernotifikasjon.kvittering;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.brukernotifikasjon.*;
import no.nav.veilarbaktivitet.brukernotifikasjon.avslutt.AvsluttBrukernotifikasjonCron;
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.AktivitetVarsel;
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.SendBrukernotifikasjonCron;
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.VarselDAO;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static no.nav.veilarbaktivitet.brukernotifikasjon.kvittering.EksternVarslingKvitteringConsumer.*;
import static org.junit.jupiter.api.Assertions.*;

class BrukernotifikasjonBeskjedKvitteringTest extends SpringBootTestBase {
    @Autowired
    BrukernotifikasjonService brukernotifikasjonService;

    @Autowired
    AvsluttBrukernotifikasjonCron avsluttBrukernotifikasjonCron;

    @Autowired
    SendBrukernotifikasjonCron sendBrukernotifikasjonCron;

    @Autowired
    VarselDAO varselDao;

    TestConsumer brukerVarselConsumer;

    @Autowired
    NamedParameterJdbcTemplate jdbc;

    @Autowired
    EksternVarslingKvitteringConsumer eksternVarslingKvitteringConsumer;

    @Autowired
    MeterRegistry meterRegistry;

    @Value("${app.env.aktivitetsplan.basepath}")
    String basepath;

    @Autowired
    BrukernotifikasjonAssertsConfig brukernotifikasjonAssertsConfig;
    BrukernotifikasjonAsserts brukernotifikasjonAsserts;

    private final static String BESSKJED_KVOTERINGS_PREFIX = "B-veilarbaktivitet-";

    BrukernotifikasjonAsserts getBrukernotifikasjonAsserts() {
        if (brukernotifikasjonAsserts != null) {
            return brukernotifikasjonAsserts;
        }
        return new BrukernotifikasjonAsserts(brukernotifikasjonAssertsConfig);
    }

    @BeforeEach
    public void setUp() {
        DbTestUtils.cleanupTestDb(jdbc.getJdbcTemplate());
        brukernotifikasjonAsserts = getBrukernotifikasjonAsserts();
        brukerVarselConsumer = brukernotifikasjonAsserts.getBrukervarselConsumer();
    }

    @AfterEach
    public void assertNoUnkowns() {
        brukerVarselConsumer.getConsumer().unsubscribe();
        assertTrue(WireMock.findUnmatchedRequests().isEmpty());
    }

    @SneakyThrows
    @Test
    void notifikasjonsstatus_tester() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettAktivitet(mockBruker, skalOpprettes);
        assertEquals(0, varselDao.hentAntallUkvitterteVarslerForsoktSendt(-1));

        var oppgaveRecord = opprettOppgave(mockBruker, aktivitetDTO);
        String eventId = oppgaveRecord.getVarselId();

        assertVarselStatusErSendt(eventId);
        assertEquals(1, varselDao.hentAntallUkvitterteVarslerForsoktSendt(-1));
        assertEksternVarselStatus(eventId, VarselKvitteringStatus.IKKE_SATT);
        skalIkkeBehandleMedAnnenBestillingsId(eventId);
        infoOgOVersendtSkalIkkeEndreStatus(eventId, VarselKvitteringStatus.IKKE_SATT);
        consumAndAssertStatus(eventId, okStatus(eventId), VarselKvitteringStatus.OK);
        assertEquals(0, varselDao.hentAntallUkvitterteVarslerForsoktSendt(-1));
        infoOgOVersendtSkalIkkeEndreStatus(eventId, VarselKvitteringStatus.OK);
        consumAndAssertStatus(eventId, feiletStatus(eventId), VarselKvitteringStatus.FEILET);
        consumAndAssertStatus(eventId, okStatus(eventId), VarselKvitteringStatus.FEILET);
        infoOgOVersendtSkalIkkeEndreStatus(eventId, VarselKvitteringStatus.FEILET);

        Gauge gauge = meterRegistry.find("brukernotifikasjon_mangler_kvittering").gauge();
        sendBrukernotifikasjonCron.countForsinkedeVarslerSisteDognet();
        assertEquals(0, gauge.value());


        String brukernotifikasjonId = eventId;
        val ugyldigstatus = new ConsumerRecord<>("VarselKviteringToppic", 1, 1, brukernotifikasjonId, status(eventId, "ugyldig_status"));
        assertThrows(IllegalArgumentException.class, () -> eksternVarslingKvitteringConsumer.consume(ugyldigstatus));

        String feilprefixId = "feilprefix-" + eventId;

        DoknotifikasjonStatus melding = DoknotifikasjonStatus
                .newBuilder()
                .setStatus(OVERSENDT)
                .setBestillingsId(feilprefixId)
                .setBestillerId("veilarbaktivitet")
                .setMelding("her er en melding")
                .setDistribusjonId(1L)
                .build();
        val feilPrefix = new ConsumerRecord<>("VarselKviteringToppic", 1, 1, feilprefixId, melding);
        assertThrows(IllegalArgumentException.class, () -> eksternVarslingKvitteringConsumer.consume(feilPrefix));


        assertVarselStatusErSendt(eventId);//SKAl ikke ha endret seg
        assertEksternVarselStatus(eventId, VarselKvitteringStatus.FEILET); //SKAl ikke ha endret seg
    }

    private void infoOgOVersendtSkalIkkeEndreStatus(String eventId, VarselKvitteringStatus expectedVarselKvitteringStatus) {
        consumAndAssertStatus(eventId, infoStatus(eventId), expectedVarselKvitteringStatus);
        consumAndAssertStatus(eventId, oversendtStatus(eventId), expectedVarselKvitteringStatus);
    }

    private void skalIkkeBehandleMedAnnenBestillingsId(String eventId) {
        DoknotifikasjonStatus statusMedAnnenBestillerId = okStatus(eventId);
        statusMedAnnenBestillerId.setBestillerId("annen_bestillerid");

        consumAndAssertStatus(eventId, statusMedAnnenBestillerId, VarselKvitteringStatus.IKKE_SATT);
    }


    private void consumAndAssertStatus(String eventId, DoknotifikasjonStatus message, VarselKvitteringStatus expectedEksternVarselStatus) {
        String brukernotifikasjonId = BESSKJED_KVOTERINGS_PREFIX + eventId;
        eksternVarslingKvitteringConsumer.consume(new ConsumerRecord<>("VarselKviteringToppic", 1, 1, brukernotifikasjonId, message));

        assertVarselStatusErSendt(eventId);
        assertEksternVarselStatus(eventId, expectedEksternVarselStatus);
    }

    private void assertVarselStatusErSendt(String eventId) {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("eventId", eventId);
        String status = jdbc.queryForObject("SELECT STATUS from BRUKERNOTIFIKASJON where BRUKERNOTIFIKASJON_ID = :eventId", param, String.class);//TODO fiks denne når vi eksponerer det ut til apiet
        assertEquals(VarselStatus.SENDT.name(), status);
    }

    private void assertEksternVarselStatus(String eventId, VarselKvitteringStatus expectedVarselStatus) {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("eventId", eventId);
        String status = jdbc.queryForObject("SELECT VARSEL_KVITTERING_STATUS from BRUKERNOTIFIKASJON where BRUKERNOTIFIKASJON_ID = :eventId", param, String.class);//TODO fiks denne når vi eksponerer det ut til apiet
        assertEquals(expectedVarselStatus.name(), status);
    }

    private DoknotifikasjonStatus status(String eventId, String status) {
        String bestillingsId = BESSKJED_KVOTERINGS_PREFIX + eventId;
        return DoknotifikasjonStatus
                .newBuilder()
                .setStatus(status)
                .setBestillingsId(bestillingsId)
                .setBestillerId("veilarbaktivitet")
                .setMelding("her er en melding")
                .setDistribusjonId(1L)
                .build();
    }

    private DoknotifikasjonStatus okStatus(String bestillingsId) {
        return status(bestillingsId, FERDIGSTILT);
    }
    private DoknotifikasjonStatus feiletStatus(String bestillingsId) {
        return status(bestillingsId, FEILET);
    }
    private DoknotifikasjonStatus infoStatus(String bestillingsId) {
        return status(bestillingsId, INFO);
    }
    private DoknotifikasjonStatus oversendtStatus(String eventId) {
        return status(eventId, OVERSENDT);
    }

    private OpprettVarselDto opprettOppgave(MockBruker mockBruker, AktivitetDTO aktivitetDTO) {
        brukernotifikasjonService.opprettVarselPaaAktivitet(new AktivitetVarsel(
                Long.parseLong(aktivitetDTO.getId()),
                Long.parseLong(aktivitetDTO.getVersjon()),
                mockBruker.getAktorId(),
                "Testvarsel",
                VarselType.STILLING_FRA_NAV)
        );

        sendBrukernotifikasjonCron.sendBrukernotifikasjoner();
        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner();

        OpprettVarselDto oppgave = brukerVarselConsumer.waitForOpprettVarsel();

        assertEquals(mockBruker.getFnr(), oppgave.getIdent());
        assertEquals(basepath + "/aktivitet/vis/" + aktivitetDTO.getId(), oppgave.getLink());
        return oppgave;
    }
}
