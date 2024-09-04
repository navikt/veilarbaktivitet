package no.nav.veilarbaktivitet;


import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.getunleash.Unleash;
import io.restassured.RestAssured;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import no.nav.poao_tilgang.poao_tilgang_test_wiremock.PoaoTilgangWiremock;
import no.nav.veilarbaktivitet.aktivitet.HistorikkService;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaJsonTemplate;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.mock_nav_modell.NavMockService;
import no.nav.veilarbaktivitet.stilling_fra_nav.RekrutteringsbistandStatusoppdatering;
import no.nav.veilarbaktivitet.stilling_fra_nav.StillingFraNavTestService;
import no.nav.veilarbaktivitet.util.AktivitetTestService;
import no.nav.veilarbaktivitet.util.KafkaTestService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@WireMockTest
public abstract class SpringBootTestBase {
    @Autowired
    protected KafkaTestService kafkaTestService;

    private static WireMockServer getWireMockServer() {
        var wiremockServer = new WireMockServer(wireMockConfig().dynamicPort().dynamicHttpsPort());
        wiremockServer.start();
        return wiremockServer;
    }

    private static final PoaoTilgangWiremock poaoTilgangWiremock = new PoaoTilgangWiremock(0, "", MockNavService.NAV_CONTEXT);
    public static final WireMockServer wireMock = getWireMockServer();

    @Autowired
    private StillingFraNavTestService stillingFraNavTestService;
    protected AktivitetTestService aktivitetTestService;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private LockProvider lockProvider;

    @Autowired
    protected Unleash unleash;

    @Autowired
    private KafkaTemplate<String, String> stringStringKafkaTemplate;

    @Autowired
    private KafkaJsonTemplate<RekrutteringsbistandStatusoppdatering> navCommonKafkaJsonTemplate;

    @Autowired
    protected NavMockService navMockService;

    @Value("${topic.inn.aktivitetskort}")
    private String aktivitetskortTopic;

    @Value("${topic.inn.rekrutteringsbistandStatusoppdatering}")
    private String innRekrutteringsbistandStatusoppdateringTopic;

    @LocalServerPort
    protected int port;

    @BeforeEach
    public void setup() {
        RestAssured.port = port;
        DbTestUtils.cleanupTestDb(jdbcTemplate);
        JdbcTemplateLockProvider l = (JdbcTemplateLockProvider) lockProvider;
        l.clearCache();
        aktivitetTestService = new AktivitetTestService(stillingFraNavTestService, port, innRekrutteringsbistandStatusoppdateringTopic, kafkaTestService, stringStringKafkaTemplate, navCommonKafkaJsonTemplate, aktivitetskortTopic);
    }

    @DynamicPropertySource
    public static void setWiremockPorts(DynamicPropertyRegistry registry) {
        registry.add("app.env.poao_tilgang.url", () -> poaoTilgangWiremock.getWireMockServer().baseUrl());
        registry.add("wiremock.server.port", () -> wireMock.port());
    }
}
