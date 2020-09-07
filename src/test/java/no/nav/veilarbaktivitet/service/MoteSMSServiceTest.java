package no.nav.veilarbaktivitet.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.veilarbaktivitet.AktivitetDataTestBuilder;
import no.nav.veilarbaktivitet.config.ApplicationTestConfig;
import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.veilarbaktivitet.db.dao.MoteSmsDAO;
import no.nav.veilarbaktivitet.domain.*;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;


@EnableAutoConfiguration
@Import(ApplicationTestConfig.class)
@RunWith(MockitoJUnitRunner.class)
public class MoteSMSServiceTest {

    @Mock
    private UnleashService unleash;

    @Mock
    private JmsTemplate varselQueueJsm;

    @Captor
    private ArgumentCaptor<SmsAktivitetData> smsCapture;

    @Spy
    @InjectMocks
    private VarselQueService varselQueue;

    private final Date before = createDate(1);
    private final Date earlyCuttoff = createDate(2);
    private final Date betwheen = createDate(3);
    private final Date betwheen2 = createDate(4);
    private final Date lateCuttof = createDate(5);
    private final Date after = createDate(6);
    private static final Person.AktorId AKTOR_ID = Person.aktorId("1234");


    private final JdbcTemplate jdbcTemplate = LocalH2Database.getDb();
    private final Database database = new Database(jdbcTemplate);

    private final MoteSmsDAO moteSmsDAO = new MoteSmsDAO(database);

    private final AktivitetDAO aktivitetDAO = new AktivitetDAO(database);

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private MoteSMSService moteSMSService;

    @BeforeClass
    public static void settup() {
        System.setProperty("AKTIVITETSPLAN_URL", "aktivitesplan_url");
    }

    @Before
    public void cleanUp() {
        DbTestUtils.cleanupTestDb(jdbcTemplate);

        moteSMSService = new MoteSMSService(varselQueue,
                moteSmsDAO,
                unleash,
                meterRegistry
        );

    }

    @Before
    public void setupMock() {
        when(unleash.isEnabled(eq("veilarbaktivitet.motesms"), any())).thenReturn(true);
    }

    @Test
    public void skalIkkeFeileMedTomListe() {
        moteSMSService.sendServicemelding();

        assertThatMeldingerSendt(0,0);
    }

    @Test
    public void skalIkkeSendeFlereVarselrForEnAktivitet() {
        insertMote(1, betwheen);
        insertMote(1, betwheen2);
        moteSMSService.sendServicemelding(earlyCuttoff, lateCuttof);

        assertThatMeldingerSendt(1,0);

        verify(varselQueue).sendMoteSms(smsCapture.capture());
        SmsAktivitetData value = smsCapture.getValue();
        assertThat(value.getAktorId()).isEqualTo(AKTOR_ID.get());
        assertThat(betwheen2).isEqualTo(value.getMoteTidAktivitet()); //må stå denne veien
        assertThat(value.getAktivitetId()).isEqualTo(1);
    }

    @Test
    public void skalSendeSMSForAktivteterMellom() {
        insertMote(1, betwheen);
        insertMote(2, betwheen);

        moteSMSService.sendServicemelding(earlyCuttoff, lateCuttof);

        assertThatMeldingerSendt(2,0);
    }


    @Test
    public void skalIkkeSendeSmsForAktiviteterUtenfor() {
        insertMote(1, after);
        insertMote(2, before);

        moteSMSService.sendServicemelding(earlyCuttoff, lateCuttof);

        assertThatMeldingerSendt(0,0);
    }

    @Test
    public void skalIkkeSendeFlereGanger() {
        insertMote(1, betwheen);
        moteSMSService.sendServicemelding(earlyCuttoff, lateCuttof);
        assertThatMeldingerSendt(1,0);

        insertMote(1, betwheen);
        moteSMSService.sendServicemelding(earlyCuttoff, lateCuttof);
        assertThatMeldingerSendt(1,0);
    }


    @Test
    public void skalSendePaaNyttHvisOppdatert() {
        insertMote(1, betwheen);

        moteSMSService.sendServicemelding(earlyCuttoff, lateCuttof);
        assertThatMeldingerSendt(1,0);

        insertMote(1, betwheen2);

        moteSMSService.sendServicemelding(earlyCuttoff, lateCuttof);
        assertThatMeldingerSendt(1,1);

    }

    @Test
    public void skalSendePaAlleUntatAvbrutt() {

        AktivitetStatus[] values = AktivitetStatus.values();
        for (int i = 0; i < values.length; i++) {
            insertAktivitet(i, betwheen, AktivitetTypeData.MOTE, values[i]);
        }

        moteSMSService.sendServicemelding(earlyCuttoff, lateCuttof);

        assertThatMeldingerSendt(values.length - 1,0);

    }

    @Test
    public void skalIkkeSendeForAndreAktivitetTyper() {
        AktivitetTypeData[] values = AktivitetTypeData.values();

        for (int i = 0; i < values.length; i++) {
            insertAktivitet(i, betwheen, values[i], AktivitetStatus.GJENNOMFORES);
        }

        moteSMSService.sendServicemelding(before, after);

        assertThatMeldingerSendt(1,0);
    }

    private void insertMote(long id, Date fraDato) {
        insertAktivitet(id, fraDato, AktivitetTypeData.MOTE, AktivitetStatus.GJENNOMFORES);
    }

    private void insertAktivitet(long id, Date fraDato, AktivitetTypeData type, AktivitetStatus aktivitetStatus) {
        AktivitetData aktivitet = AktivitetDataTestBuilder
                .nyAktivitet()
                .id(id)
                .aktivitetType(type)
                .status(aktivitetStatus)
                .fraDato(fraDato)
                .aktorId(AKTOR_ID.get())
                .build();

        aktivitetDAO.insertAktivitet(aktivitet);
    }

    private void assertThatMeldingerSendt(int nyeMoterMelding, int moterOppdatertMelding) {
        int totMeldinger = nyeMoterMelding + moterOppdatertMelding;

        verify(varselQueue, times(totMeldinger)).sendMoteSms(any());
        verify(varselQueueJsm, times(totMeldinger)).send(any());

        assertThatGjeldendeInneholder(nyeMoterMelding);
        assertThatHistorikkInneholder(totMeldinger);
    }

    private void assertThatGjeldendeInneholder(int antall) {
        int size = hentGjeldendeMoter().size();

        assertThat(size).isEqualTo(antall);
    }

    private void assertThatHistorikkInneholder(int antall) {
        int size = hentVarselHistorikk().size();

        assertThat(size).isEqualTo(antall);
    }
    private java.util.List<java.util.Map<String, Object>> hentGjeldendeMoter() {
        return jdbcTemplate.queryForList("select * from GJELDENDE_MOTE_SMS");
    }

    private List<Map<String, Object>> hentVarselHistorikk() {
        return jdbcTemplate.queryForList("select * from MOTE_SMS_HISTORIKK");
    }

    private Date createDate(int hour) {
        return Date.from(LocalDateTime.of(2016, 1, 1, hour, 1).toInstant(ZoneOffset.UTC));
    }
}
