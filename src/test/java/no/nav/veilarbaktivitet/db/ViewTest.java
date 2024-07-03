package no.nav.veilarbaktivitet.db;

import io.zonky.test.db.postgres.junit5.PreparedDbExtension;
import lombok.SneakyThrows;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.LocalDatabaseSingleton;
import org.json.JSONArray;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ViewTest {

    public static Stream<String> views() {
        return Stream.of(
                "DVH_TRANSAKSJON_TYPE",
                "DVH_AKT_LIVSLOPSTATUS_TYPE",
                "DVH_AKTIVITET_TYPE",
                "DVH_STILLINGSOK_ETIKETT_TYPE",
                "DVH_AKTIVITET",
                "DVH_STILLINGSOK_AKTIVITET",
                "DVH_SOKEAVTALE_AKTIVITET",
                "DVH_EGENAKTIVITET",
                "DVH_MOTE",
                "DVH_KANAL_TYPE",
                "DVH_SLETTEDE_AKTIVITETER",
                "DVH_JOBB_STATUS_TYPE",
                "DVH_IJOBB",
                "DVH_STILLING_FRA_NAV_AKTIVITET");
    }

    @RegisterExtension
    static public PreparedDbExtension localdataBase = LocalDatabaseSingleton.INSTANCE.getPostgres();

    private static final long antallViews = views().count();

    @Test
    void database_skal_ha_riktig_antall_views() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(localdataBase.getTestDatabase());
        var count = jdbcTemplate.queryForList(
                "SELECT " +
                "COUNT(*) AS VIEW_COUNT " +
                "FROM INFORMATION_SCHEMA.VIEWS " +
                "where TABLE_SCHEMA = 'public';"
        ).get(0).get("VIEW_COUNT");

        assertThat(count).isEqualTo(antallViews);
    }

    @ParameterizedTest
    @MethodSource("views")
    void view_eksisterer(String viewName) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(localdataBase.getTestDatabase());
        List<Map<String, Object>> viewData = jdbcTemplate.queryForList("SELECT * FROM " + viewName + ";");

        assertThat(viewData).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("views")
    void view_skal_reflektere_kolonner_i_tabell(String viewName) {
        String kolonneData = jsonFormatter(JsonUtils.toJson(hentKolonneDataForView(viewName)));
        String kolonneDataFasit = jsonFormatter(lesInnholdFraFil("view-meta-data/" + viewName.toLowerCase() + ".json"));

        assertThat(kolonneData).isEqualTo(kolonneDataFasit);
    }

    private List<Map<String, Object>> hentKolonneDataForView(String view) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(localdataBase.getTestDatabase());
        return jdbcTemplate.queryForList(
                "SELECT " +
                "COLUMN_NAME, " +
                "DATA_TYPE, " +
                "CHARACTER_MAXIMUM_LENGTH " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = '" + view.toLowerCase() + "';"
        );
    }

    /*
     select column_name, columns.data_type, columns.character_maximum_length
 from information_schema.columns;
     */

    @SneakyThrows
    private static String jsonFormatter(String jsonArray) {
        return new JSONArray(jsonArray).toString();
    }


    private static String lesInnholdFraFil(String filNavn) {
        return new Scanner(ViewTest.class.getClassLoader().getResourceAsStream(filNavn), StandardCharsets.UTF_8).useDelimiter("\\A").next();
    }
}
