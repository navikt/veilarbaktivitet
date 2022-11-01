package no.nav.veilarbaktivitet.aktivitet.dto.filterTags;

import no.nav.common.json.JsonUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.List;

public class FiltersTest {

    @Test
    public void should_be_able_to_serialize() {
        var stuff = Filters.listOf(
            Filters.of("asds", "adas"),
            Filters.of("asds", null), // Null will be removed
            Filters.of("asds", 0), // Number not allowed
            Filters.of("asds", "null")
        );
        var result = JsonUtils.toJson(stuff);
        Assertions.assertThat(result).isEqualTo("""
            [{"kategori":"asds","verdi":"adas"},{"kategori":"asds","verdi":"null"}]
        """.trim());
    }

}