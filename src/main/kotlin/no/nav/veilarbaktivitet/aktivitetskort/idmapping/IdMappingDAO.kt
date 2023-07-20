package no.nav.veilarbaktivitet.aktivitetskort.idmapping

import no.nav.veilarbaktivitet.arena.model.ArenaId
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import java.sql.ResultSet
import java.util.*

@Service
open class IdMappingDAO (
    val db: NamedParameterJdbcTemplate
) {

    fun insert(idMapping: IdMapping) {
        val params = MapSqlParameterSource()
            .addValue("arenaId", idMapping.arenaId.id())
            .addValue("aktivitetId", idMapping.aktivitetId)
            .addValue("funksjonellId", idMapping.funksjonellId.toString())
        db.update(
            """
                INSERT INTO ID_MAPPINGER (EKSTERN_REFERANSE_ID, AKTIVITET_ID, FUNKSJONELL_ID)
                VALUES (:arenaId, :aktivitetId, :funksjonellId)
                
                """.trimIndent(), params
        )
    }

    fun getAktivitetId(arenaId: ArenaId): Optional<Long> {
        val params = MapSqlParameterSource().addValue("arenaId", arenaId.id())
        return db.queryForList(
            """
                SELECT AKTIVITET_ID FROM ID_MAPPINGER WHERE EKSTERN_REFERANSE_ID = :arenaId
                
                """.trimIndent(), params, Long::class.java
        )
            .stream()
            .findFirst()
    }

    fun getMappingsByFunksjonellId(ids: List<UUID>): Map<UUID, IdMapping> {
        if (ids.isEmpty()) return emptyMap()
        val stringIds = ids.stream().map { obj: UUID -> obj.toString() }.toList()
        val params = MapSqlParameterSource()
            .addValue("arenaIds", stringIds)
        val idList = db.query(
            """
                SELECT * FROM ID_MAPPINGER where ID_MAPPINGER.FUNKSJONELL_ID in (:arenaIds)
                
                """.trimIndent(), params, rowmapper
        )
        return idList.stream()
            .reduce(HashMap(), { mapping: HashMap<UUID, IdMapping>, singleIdMapping: IdMapping ->
                mapping[singleIdMapping.funksjonellId] = singleIdMapping
                mapping
            }) { accumulatedMappings: HashMap<UUID, IdMapping>, nextSingleMapping: HashMap<UUID, IdMapping>? ->
                accumulatedMappings.putAll(
                    nextSingleMapping!!
                )
                accumulatedMappings
            }
    }

    fun getMappings(ids: List<ArenaId>): Map<ArenaId, IdMapping> {
        if (ids.isEmpty()) return HashMap()
        val stringIds = ids.stream().map { obj: ArenaId -> obj.id() }.toList()
        val params = MapSqlParameterSource()
            .addValue("arenaIds", stringIds)
        val idList = db.query(
            """
                SELECT * FROM ID_MAPPINGER where ID_MAPPINGER.EKSTERN_REFERANSE_ID in (:arenaIds)
                
                """.trimIndent(), params, rowmapper
        )
        return idList.stream()
            .reduce(HashMap(), { mapping: HashMap<ArenaId, IdMapping>, singleIdMapping: IdMapping ->
                mapping[singleIdMapping.arenaId] = singleIdMapping
                mapping
            }) { accumulatedMappings: HashMap<ArenaId, IdMapping>, nextSingleMapping: HashMap<ArenaId, IdMapping>? ->
                accumulatedMappings.putAll(
                    nextSingleMapping!!
                )
                accumulatedMappings
            }
    }

    var rowmapper = RowMapper { rs: ResultSet, _: Int ->
        IdMapping(
            ArenaId(rs.getString("EKSTERN_REFERANSE_ID")),
            rs.getLong("AKTIVITET_ID"),
            UUID.fromString(rs.getString("FUNKSJONELL_ID"))
        )
    }
}
