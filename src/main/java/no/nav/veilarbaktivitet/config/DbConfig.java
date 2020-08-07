package no.nav.veilarbaktivitet.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import no.nav.common.utils.Credentials;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

import static no.nav.common.utils.NaisUtils.getCredentials;

@Configuration
@EnableTransactionManagement
public class DbConfig {

    private final EnvironmentProperties environmentProperties;

    private final Credentials oracleCredentials;

    @Autowired
    public DbConfig(EnvironmentProperties environmentProperties) {
        this.environmentProperties = environmentProperties;
        oracleCredentials = getCredentials("oracle_creds");
    }

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(environmentProperties.getDbUrl());
        config.setUsername(oracleCredentials.username);
        config.setPassword(oracleCredentials.password);
        config.setMaximumPoolSize(150);
        config.setMinimumIdle(2);

        var dataSource = new HikariDataSource(config);
        migrateDb(dataSource);
        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    public static void migrateDb(DataSource dataSource) {
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.migrate();
    }


}
