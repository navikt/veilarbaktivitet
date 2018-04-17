package no.nav.fo.veilarbaktivitet.config;


import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Inject;

@Configuration
@EnableScheduling
public class MetricsReporterConfig {
    private static final int MINUTE = 10 * 1000;

    @Inject
    JdbcTemplate jdbc;

    @Scheduled(fixedDelay = MINUTE)
    public void reportMetrics() {
        antallDuplikater();
    }

    private void antallDuplikater() {
        String innerSelect = "SELECT AKTIVITET_ID as id, count(*) as gjeldendeTeller FROM AKTIVITET WHERE GJELDENDE = 1 GROUP BY AKTIVITET_ID";
        String sql = String.format("SELECT COUNT(*) as antallDuplikater FROM (%s) WHERE gjeldendeTeller > 1", innerSelect);
        Integer antallDuplikater = jdbc.queryForObject(sql, Integer.class);

        Event event = MetricsFactory.createEvent("veilarbaktivitet.gjeldendeDuplikater");
        event.addFieldToReport("count", antallDuplikater);
        event.report();
    }
}
