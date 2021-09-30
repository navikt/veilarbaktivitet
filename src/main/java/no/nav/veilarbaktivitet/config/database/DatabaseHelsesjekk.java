package no.nav.veilarbaktivitet.config.database;

import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseHelsesjekk implements HealthCheck {

    private final JdbcTemplate db;

    public DatabaseHelsesjekk(JdbcTemplate db) {
        this.db = db;
    }

    @Override
    public HealthCheckResult checkHealth() {
        try {
            db.query("SELECT 1 FROM DUAL", resultSet -> {});
            return HealthCheckResult.healthy();
        } catch (Exception e) {
            return HealthCheckResult.unhealthy("Fikk ikke kontakt med databasen", e);
        }
    }
}
