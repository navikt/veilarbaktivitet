package no.nav.veilarbaktivitet.avtaltMedNav.varsel;

import no.nav.veilarbaktivitet.config.ApplicationTestConfig;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jms.core.JmsTemplate;

import static org.junit.jupiter.api.Assertions.assertTrue;

@EnableAutoConfiguration
@Import(ApplicationTestConfig.class)
@RunWith(MockitoJUnitRunner.class)
public class AvtaltVarselServiceTest {

    private final JdbcTemplate jdbcTemplate = LocalH2Database.getDb();

    @Mock
    private JmsTemplate oppgaveHenvendelseQueue;

    @Mock
    private JmsTemplate varselMedHandlingQueue;

    @Mock
    private JmsTemplate stopVarselQueue;

    private AvtaltVarselService service;

    @BeforeClass
    public static void setup() {
        System.setProperty("AKTIVITETSPLAN_URL", "aktivitesplan_url");
    }

    @Before
    public void cleanUp() {
        DbTestUtils.cleanupTestDb(jdbcTemplate);

        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
        transactionManager.setDataSource(jdbcTemplate.getDataSource());

        final AvtaltVarselRepository repository = new AvtaltVarselRepository(jdbcTemplate);
        final AvtaltVarselMQClient client = new AvtaltVarselMQClient(oppgaveHenvendelseQueue, varselMedHandlingQueue, stopVarselQueue);
        final AvtaltVarselHandler handler = new AvtaltVarselHandler(client, repository);
        service = new AvtaltVarselService(handler, repository);
    }

    @Test
    public void firstTest() {
        assertTrue(true);
    }


}
