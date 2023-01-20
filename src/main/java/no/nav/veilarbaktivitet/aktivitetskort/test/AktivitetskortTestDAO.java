package no.nav.veilarbaktivitet.aktivitetskort.test;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AktivitetskortTestDAO {

    private final NamedParameterJdbcTemplate template;

    public boolean harSettAktivitet(@NonNull UUID funksjonellId) {
        var params = new MapSqlParameterSource()
                .addValue("funksjonellId", funksjonellId.toString());

        var antall = template.queryForObject(
                "SELECT COUNT(*) FROM AKTIVITETSKORT_FUNKSJONELL_ID_TEST WHERE FUNKSJONELL_ID = :funksjonellId",
                params,
                int.class
        );
        return antall != null && antall > 0;
    }

    public void lagreFunksjonellId(UUID funksjonellId) {
        var params = new MapSqlParameterSource()
                .addValue("funksjonellId", funksjonellId.toString());

        template.update("""
                INSERT INTO AKTIVITETSKORT_FUNKSJONELL_ID_TEST(FUNKSJONELL_ID)
                VALUES (:funksjonellId)
                """, params);
    }
}
