package no.nav.veilarbaktivitet.avtalt_med_nav.varsel;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.nav.common.types.identer.NavIdent;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.MetricService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.avtalt_med_nav.*;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.veilarbaktivitet.config.database.Database;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jms.core.JmsTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Ignore
@RunWith(MockitoJUnitRunner.class)
public class AvtaltVarselServiceTest {
    private final JdbcTemplate jdbcTemplate = LocalH2Database.getDb();

    @Mock
    private JmsTemplate oppgaveHenvendelseQueue;

    @Mock
    private JmsTemplate varselMedHandlingQueue;

    @Mock
    private JmsTemplate stopVarselQueue;

    @Mock
    private BrukernotifikasjonService brukernotifikasjonService;

    private ForhaandsorienteringDAO forhaandsorienteringDAO;
    private final MetricService metricService = mock(MetricService.class);
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private AktivitetDAO aktivitetDAO;
    private AvtaltVarselService avtaltVarselService;
    private AvtaltMedNavService avtaleService;


    @BeforeClass
    public static void setup() {
        System.setProperty("AKTIVITETSPLAN_URL", "aktivitesplan_url");
    }

    @Before
    public void cleanUp() {
        DbTestUtils.cleanupTestDb(jdbcTemplate);

        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
        transactionManager.setDataSource(jdbcTemplate.getDataSource());
        final Database database = new Database(jdbcTemplate);

        forhaandsorienteringDAO = new ForhaandsorienteringDAO(database);

        aktivitetDAO = new AktivitetDAO(database);


        avtaleService = new AvtaltMedNavService(metricService, aktivitetDAO, forhaandsorienteringDAO, meterRegistry, brukernotifikasjonService);

        final AvtaltVarselMQClient client = new AvtaltVarselMQClient(oppgaveHenvendelseQueue, varselMedHandlingQueue, stopVarselQueue);
        final AvtaltVarselHandler handler = new AvtaltVarselHandler(client, forhaandsorienteringDAO);
        avtaltVarselService = new AvtaltVarselService(handler, forhaandsorienteringDAO);
    }

    @Test
    public void sendVarsel_enForhaandsorienteringTilgjengelig_skalSetteVarselId() {
        AktivitetDTO saved = createAktivitetMedForhaandsorientering(
                Person.aktorId("1234"),
                NavIdent.of("V123"),
                "tekst");

        avtaltVarselService.sendVarsel();

        verify(varselMedHandlingQueue).send(any());
        verify(oppgaveHenvendelseQueue).send(any());
        verifySentId(saved.getForhaandsorientering().getId());
    }

    @Test
    public void sendVarsel_toForhaandsorienteringerTilgjengelig_skalSetteVarelIdPaaBegge() {
        AktivitetDTO one = createAktivitetMedForhaandsorientering(
                Person.aktorId("1"),
                NavIdent.of("V1"),
                "1"
        );

        AktivitetDTO two = createAktivitetMedForhaandsorientering(
                Person.aktorId("2"),
                NavIdent.of("V2"),
                "2"
        );

        avtaltVarselService.sendVarsel();

        verify(varselMedHandlingQueue, times(2)).send(any());
        verify(oppgaveHenvendelseQueue, times(2)).send(any());

        verifySentId(one.getForhaandsorientering().getId());
        verifySentId(two.getForhaandsorientering().getId());
    }

    @Test
    public void sendVarsel_enForhaandsorienteringTilgjengelig_sendesKunEnGang() {
        AktivitetDTO one = createAktivitetMedForhaandsorientering(
                Person.aktorId("1"),
                NavIdent.of("V1"),
                "1"
        );

        avtaltVarselService.sendVarsel();
        avtaltVarselService.sendVarsel();

        verify(varselMedHandlingQueue, times(1)).send(any());
        verify(oppgaveHenvendelseQueue, times(1)).send(any());
        verifySentId(one.getForhaandsorientering().getId());
    }

    @Test
    public void sendVarsel_enForhaandsorienteringTilgjengelig_SendtSaaStoppetSetterStoppetDato() {
        AktivitetDTO one = createAktivitetMedForhaandsorientering(
                Person.aktorId("1"),
                NavIdent.of("V1"),
                "1"
        );

        avtaltVarselService.sendVarsel();

        verify(varselMedHandlingQueue, times(1)).send(any());
        verify(oppgaveHenvendelseQueue, times(1)).send(any());
        verifySentId(one.getForhaandsorientering().getId());

        forhaandsorienteringDAO.settVarselFerdig(one.getForhaandsorientering().getId());
        avtaltVarselService.stoppVarsel();

        Forhaandsorientering savedOrientering = forhaandsorienteringDAO.getById(
                one.getForhaandsorientering().getId()
        );

        assertNotNull(savedOrientering.getVarselSkalStoppesDato());
        assertNotNull(savedOrientering.getVarselStoppetDato());


    }

    private AktivitetDTO createAktivitetMedForhaandsorientering(Person.AktorId aktorId, NavIdent veilederIdent, String tekst) {
        final AktivitetData aktivitet = aktivitetDAO.opprettNyAktivitet(AktivitetDataTestBuilder.nyEgenaktivitet()
                .withAktorId(aktorId.get()));

        var fhoDTO = ForhaandsorienteringDTO.builder()
                .type(Type.SEND_FORHAANDSORIENTERING)
                .tekst("tekst").build();

        AvtaltMedNavDTO avtalt = new AvtaltMedNavDTO()
                .setForhaandsorientering(fhoDTO)
                .setAktivitetVersjon(aktivitet.getVersjon());

        return avtaleService.opprettFHO(avtalt, aktivitet.getId(), aktorId, veilederIdent);
    }

    private void verifySentId(String id) {
        Forhaandsorientering savedOrientering = forhaandsorienteringDAO.getById(id);
        assertNotNull(savedOrientering);
        assertNotNull(savedOrientering.getVarselId());
    }

}
