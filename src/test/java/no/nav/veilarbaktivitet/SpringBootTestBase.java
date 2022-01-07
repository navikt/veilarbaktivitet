package no.nav.veilarbaktivitet;


import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.util.AktivitetTestService;
import no.nav.veilarbaktivitet.util.KafkaTestService;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 0)
public class SpringBootTestBase {
    @Autowired
    protected KafkaTestService testService;

    @Autowired
    protected AktivitetTestService testAktivitetservice;

    @Autowired
    protected JdbcTemplate jdbc;

    @LocalServerPort
    protected int port;

    @Before
    public void setUp() {
        DbTestUtils.cleanupTestDb(jdbc);
    }
}
