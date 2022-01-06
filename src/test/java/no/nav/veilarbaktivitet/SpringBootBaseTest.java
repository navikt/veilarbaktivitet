package no.nav.veilarbaktivitet;


import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.util.AktivitetTestService;
import no.nav.veilarbaktivitet.util.KafkaTestService;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

public class SpringBootBaseTest {
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
