package no.nav.fo.veilarbaktivitet.db;

import no.nav.apiapp.util.JsonUtils;
import no.nav.fo.DatabaseTestContext;
import no.nav.fo.IntegrasjonsTest;
import org.json.JSONArray;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(value = Parameterized.class)
public class ViewTest extends IntegrasjonsTest {

    private static final String[] views = new String[]{
        "DVH_TRANSAKSJON_TYPE",
        "DVH_AKT_LIVSLOPSTATUS_TYPE",
        "DVH_AKTIVITET_TYPE",
        "DVH_STILLINGSOK_ETIKETT_TYPE",
        "DVH_AKTIVITET",
        "DVH_STILLINGSOK_AKTIVITET",
        "DVH_SOKEAVTALE_AKTIVITET",
        "DVH_EGENAKTIVITET"
    };

    private static final int antallViews = 8;

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Parameter(value = 0)
    public String viewName;

    @Parameters(name = "{0}")
    public static Object[] data() {
        Object[] objects = Arrays.stream(views).map(view -> new Object[]{
            view,
        }).toArray();

        return objects;
    }

    @Test
    public void database_skal_ha_riktig_antall_views() {
        long count = (long) jdbcTemplate.queryForList("" +
            "SELECT " +
                "COUNT(*) AS VIEW_COUNT " +
            "FROM INFORMATION_SCHEMA.VIEWS;"
        ).get(0).get("VIEW_COUNT");

        assertThat(count).isEqualTo(antallViews);
    }

    @Test
    public void view_eksisterer() {
        List<Map<String, Object>> viewData = jdbcTemplate.queryForList("SELECT * FROM " + viewName + ";");

        assertThat(viewData).isNotNull();
    }

    @Test
    public void view_skal_reflektere_kolonner_i_tabell() {
        String kolonneData = jsonFormatter(JsonUtils.toJson(hentKolonneDataForView(viewName)));
        String kolonneDataFasit = jsonFormatter(lesInnholdFraFil("view-meta-data/" + viewName.toLowerCase() + ".json"));

        assertThat(kolonneData).isEqualTo(kolonneDataFasit);
    }

    private static String jsonFormatter(String jsonArray) {
        return new JSONArray(jsonArray).toString();
    }

    private static List<Map<String, Object>> hentKolonneDataForView(String view) {
        return new JdbcTemplate(DatabaseTestContext.buildDataSource()).queryForList(
            "SELECT " +
                "COLUMN_NAME, " +
                "TYPE_NAME, " +
                "CHARACTER_MAXIMUM_LENGTH " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = '" + view + "';"
        );
    }

    private static String lesInnholdFraFil(String filNavn) {
        return new Scanner(ViewTest.class.getClassLoader().getResourceAsStream(filNavn), "UTF-8").useDelimiter("\\A").next();
    }
}
