package no.nav.veilarbaktivitet.aktivitetskort.idmapping;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.arena.model.ArenaId;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class IdMappingDAO {

    private final NamedParameterJdbcTemplate template;

    public void insert(IdMapping idMapping) {
        var params = new MapSqlParameterSource()
                .addValue("arenaId", idMapping.areanaId().id())
                .addValue("aktivitetId", idMapping.aktivitetId())
                .addValue("funksjonellId", idMapping.funksjonellId().toString());

        template.update("""
                INSERT INTO ID_MAPPINGER (EKSTERN_REFERANSE_ID, AKTIVITET_ID, FUNKSJONELL_ID) 
                VALUES (:arenaId, :aktivitetId, :funksjonellId)
                """, params);
    }

    public Map<ArenaId, IdMapping> getMappings(List<ArenaId> ids) {
        if (ids.isEmpty()) return new HashMap<>();
        var stringIds = ids.stream().map(ArenaId::id).toList();
        var params = new MapSqlParameterSource()
                .addValue("arenaIds", String.join(",", stringIds));
        var idList = template.query("""
                SELECT * FROM ID_MAPPINGER where ID_MAPPINGER.EKSTERN_REFERANSE_ID in (:arenaIds)
                """, params, rowmapper);

        return idList.stream()
            .reduce(new HashMap<ArenaId, IdMapping>(), (mapping, singleIdMapping) -> {
                mapping.put(singleIdMapping.areanaId(), singleIdMapping);
                return mapping;
            }, (accumulatedMappings, nextSingleMapping) -> {
                accumulatedMappings.putAll(nextSingleMapping);
                return accumulatedMappings;
            });
    }

    RowMapper<IdMapping> rowmapper = (rs, rowNum) ->
            new IdMapping(
                    new ArenaId(rs.getString("EKSTERN_REFERANSE_ID")),
                    rs.getLong("AKTIVITET_ID"),
                    UUID.fromString(rs.getString("FUNKSJONELL_ID"))
            );

}
