package no.nav.veilarbaktivitet;


import io.restassured.RestAssured;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.stilling_fra_nav.StillingFraNavTestService;
import no.nav.veilarbaktivitet.util.AktivitetTestService;
import no.nav.veilarbaktivitet.util.KafkaTestService;
import no.nav.veilarbaktivitet.util.KasserTestService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
public abstract class SpringBootTestBase {
    @Autowired
    protected KafkaTestService kafkaTestService;

    @Autowired
    private StillingFraNavTestService stillingFraNavTestService;
    protected AktivitetTestService aktivitetTestService;

    protected KasserTestService kasserTestService;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private LockProvider lockProvider;

    @Autowired
    protected UnleashClient unleashClient;

    @Autowired
    private KafkaTemplate<String, String> stringStringKafkaTemplate;

    @Value("${topic.inn.aktivitetskort}")
    private String aktivitetskortTopic;

    @LocalServerPort
    protected int port;

    @BeforeEach
    public void setup() {
        RestAssured.port = port;
        DbTestUtils.cleanupTestDb(jdbcTemplate);
        JdbcTemplateLockProvider l = (JdbcTemplateLockProvider) lockProvider;
        l.clearCache();
        aktivitetTestService = new AktivitetTestService(stillingFraNavTestService, port, kafkaTestService, stringStringKafkaTemplate, aktivitetskortTopic);
        kasserTestService = new KasserTestService(port);
    }
}
