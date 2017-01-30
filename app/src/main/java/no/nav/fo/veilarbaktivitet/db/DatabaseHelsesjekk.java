package no.nav.fo.veilarbaktivitet.db;

import no.nav.fo.veilarbaktivitet.Helsesjekk;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class DatabaseHelsesjekk implements Helsesjekk {

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Override
    public void helsesjekk() {
        jdbcTemplate.queryForObject("SELECT 1 FROM DUAL", Long.class);
    }

}
