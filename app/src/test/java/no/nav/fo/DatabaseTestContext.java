package no.nav.fo;

import no.nav.dialogarena.config.fasit.DbCredentials;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.TestEnvironment;
import no.nav.fo.veilarbaktivitet.db.testdriver.TestDriver;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static no.nav.apiapp.util.StringUtils.of;
import static no.nav.fo.veilarbaktivitet.TestConfig.APPLICATION_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

@Configuration
@EnableTransactionManagement
public class DatabaseTestContext {

    public static SingleConnectionDataSource buildDataSourceFor(String miljo) {
        return of(miljo)
                .map(TestEnvironment::valueOf)
                .map(testEnvironment -> FasitUtils.getDbCredentials(testEnvironment, APPLICATION_NAME))
                .map(DatabaseTestContext::build)
                .orElseGet(DatabaseTestContext::buildDataSource);
    }

    public static SingleConnectionDataSource buildDataSource() {
        return doBuild(new DbCredentials()
                        .setUrl(TestDriver.getURL())
                        .setUsername("sa")
                        .setPassword(""),
                true
        );
    }

    public static SingleConnectionDataSource build(DbCredentials dbCredentials) {
        return doBuild(dbCredentials,false);
    }

    private static SingleConnectionDataSource doBuild(DbCredentials dbCredentials, boolean migrate) {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
        dataSource.setSuppressClose(true);
        dataSource.setUrl(dbCredentials.url);
        dataSource.setUsername(dbCredentials.username);
        dataSource.setPassword(dbCredentials.password);
        if (migrate){
            createTables(dataSource);
        }
        return dataSource;
    }

    private static void createTables(SingleConnectionDataSource singleConnectionDataSource) {
        Flyway flyway = new Flyway();
        flyway.setLocations("db/migration/veilarbaktivitetDataSource");
        flyway.setDataSource(singleConnectionDataSource);
        int migrate = flyway.migrate();
        assertThat(migrate, greaterThan(0));
    }
}
