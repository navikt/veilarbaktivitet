package no.nav.fo.veilarbaktivitet.db;

import no.nav.sbl.jdbc.DataSourceFactory;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

import static no.nav.sbl.util.EnvironmentUtils.getOptionalProperty;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
@EnableTransactionManagement(proxyTargetClass = true)
public class DatabaseContext {

    public static final String VEILARBAKTIVITETDATASOURCE_URL_PROPERTY = "VEILARBAKTIVITETDB_URL";
    public static final String VEILARBAKTIVITETDATASOURCE_USERNAME_PROPERTY = "VEILARBAKTIVITETDATASOURCE_USERNAME";
    public static final String VEILARBAKTIVITETDATASOURCE_PASSWORD_PROPERTY = "VEILARBAKTIVITETDATASOURCE_PASSWORD";

    public static void migrateDatabase(DataSource dataSource) {
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.migrate();
    }

    @Bean
    public DataSource dataSource() {
        return DataSourceFactory.dataSource()
                .url(getRequiredProperty(VEILARBAKTIVITETDATASOURCE_URL_PROPERTY))
                .username(getRequiredProperty(VEILARBAKTIVITETDATASOURCE_USERNAME_PROPERTY))
                .password(getOptionalProperty(VEILARBAKTIVITETDATASOURCE_PASSWORD_PROPERTY).orElse(""))
                .build();
    }

    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource ds) {
        return new JdbcTemplate(ds);
    }

}
