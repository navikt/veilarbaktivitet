package no.nav.fo;

import no.nav.dialogarena.config.fasit.DbCredentials;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.TestEnvironment;
import no.nav.fo.veilarbaktivitet.db.testdriver.TestDriver;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static no.nav.apiapp.util.StringUtils.of;
import static no.nav.fo.veilarbaktivitet.TestConfig.APPLICATION_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

@Configuration
@EnableTransactionManagement
public class DatabaseTestContext {

    public static AbstractDataSource buildDataSourceFor(String miljo) {
        return of(miljo)
                .map(TestEnvironment::valueOf)
                .map(testEnvironment -> FasitUtils.getDbCredentials(testEnvironment, APPLICATION_NAME))
                .map(DatabaseTestContext::build)
                .orElseGet(DatabaseTestContext::buildDataSource);
    }

    public static AbstractDataSource buildDataSource() {
        return doBuild(new DbCredentials()
                        .setUrl(TestDriver.getURL())
                        .setUsername("sa")
                        .setPassword(""),
                true
        );
    }

    private static AbstractDataSource build(DbCredentials dbCredentials) {
        return doBuild(dbCredentials,false);
    }

    private static AbstractDataSource doBuild(DbCredentials dbCredentials, boolean migrate) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(dbCredentials.url);
        dataSource.setUsername(dbCredentials.username);
        dataSource.setPassword(dbCredentials.password);
        if (migrate){
            createTables(dataSource);
        }
        return dataSource;
    }

    private static void createTables(AbstractDataSource ds) {
        Flyway flyway = new Flyway();
        flyway.setDataSource(ds);
        int migrate = flyway.migrate();
        assertThat(migrate, greaterThan(0));
    }

}
