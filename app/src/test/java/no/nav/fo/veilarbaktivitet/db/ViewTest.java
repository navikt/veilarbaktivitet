package no.nav.fo.veilarbaktivitet.db;

import no.nav.apiapp.util.JsonUtils;
import no.nav.fo.IntegrasjonsTest;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;

public class ViewTest extends IntegrasjonsTest {

    private final String[] views = new String[] {"DVH_TRANSAKSJON_TYPE", "DVH_AKTIVITET_LIVSLOPSTATUS_TYPE", "DVH_AKTIVITET_TYPE", "DVH_STILLINGSOK_ETIKETT_TYPE", "DVH_AKTIVITET", "DVH_STILLINGSOK_AKTIVITET", "DVH_SOKEAVTALE_AKTIVITET", "DVH_EGENAKTIVITET"};

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Test
    public void views_eksisterer() {
        for(String view : views) {
            List<Map<String, Object>> viewData = jdbcTemplate.queryForList("SELECT * FROM " + view + ";");
            assertThat(viewData).isNotNull();
        }
    }

    @Test
    public void view_reflekterer_tabell_kolonner() throws IOException {

        for(String view : views) {

            String kolonneData = JsonUtils.toJson(hentKolonneDataForView(view));

            String kolonneDataFasit = lesInnholdFraFil("view-meta-data/" + view.toLowerCase() + ".json");

            assertThat(kolonneData).isEqualTo(kolonneDataFasit);
        }

    }

    private List<Map<String, Object>> hentKolonneDataForView(String view) {
        return jdbcTemplate.queryForList(
            "SELECT " +
                "COLUMN_NAME, " +
                "TYPE_NAME, " +
                "CHARACTER_MAXIMUM_LENGTH " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = '" + view + "';"
        );
    }

    public String lesInnholdFraFil(String filNavn) {
        return new Scanner(getClass().getClassLoader().getResourceAsStream(filNavn), "UTF-8").useDelimiter("\\A").next();
    }
}
