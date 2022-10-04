package no.nav.veilarbaktivitet.aktivitetskort;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AktivitetsMessageDAO {
    private final NamedParameterJdbcTemplate template;

    public void insert(UUID messageId, UUID funksjonellId) {
        var params = new MapSqlParameterSource()
                .addValue("messageId", messageId.toString())
                .addValue("funksjonellId", funksjonellId.toString());

        template.update("""
                INSERT INTO AKTIVITETSKORT_MSG_ID(MESSAGE_ID, FUNKSJONELL_ID) 
                VALUES (:messageId, :funksjonellId)
                """, params);
    }

    public boolean exist(@NonNull UUID messageId) {
        var params = new MapSqlParameterSource()
            .addValue("messageId", messageId.toString());
        var antall = template.queryForObject(
                "SELECT COUNT(*) FROM AKTIVITETSKORT_MSG_ID WHERE MESSAGE_ID = :messageId",
                params,
                int.class
        );
        return antall != null && antall > 0;
    }
}
