package no.nav.veilarbaktivitet.aktivitetskort.idmapping

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.MessageSource
import no.nav.veilarbaktivitet.arena.model.ArenaId
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import java.sql.ResultSet
import java.time.LocalDateTime
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

    fun getAktivitetIder(arenaId: ArenaId): List<IdMapping> {
        val params = MapSqlParameterSource().addValue("arenaId", arenaId.id())
        return db.query(
            """
                SELECT AKTIVITET_ID, EKSTERN_REFERANSE_ID, FUNKSJONELL_ID FROM ID_MAPPINGER WHERE EKSTERN_REFERANSE_ID = :arenaId
            """.trimIndent(), params, rowmapper
        )
    }

    fun getLatestAktivitetsId(arenaId: ArenaId): Optional<Long> {
        val params = MapSqlParameterSource().addValue("arenaId", arenaId.id())
        return db.query(
            """
                SELECT ID_MAPPINGER.AKTIVITET_ID as AKTIVITET_ID 
                FROM ID_MAPPINGER JOIN AKTIVITET ON ID_MAPPINGER.AKTIVITET_ID = AKTIVITET.AKTIVITET_ID
                WHERE EKSTERN_REFERANSE_ID = :arenaId AND GJELDENDE = 1
            """.trimIndent(), params,
        ) { row, _ -> row.getLong("AKTIVITET_ID") }
            .stream()
            .findFirst()
    }

    fun getMappingsByFunksjonellId(ids: List<UUID>): Map<UUID, IdMapping> {
        if (ids.isEmpty()) return emptyMap()
        val stringIds = ids.stream().map { obj: UUID -> obj.toString() }.toList()
        val params = MapSqlParameterSource()
            .addValue("funksjonelleIder", stringIds)
        val idList = db.query(
            """
                SELECT * FROM ID_MAPPINGER where ID_MAPPINGER.FUNKSJONELL_ID in (:funksjonelleIder)
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

    fun getMappings(ids: List<ArenaId>): Map<ArenaId, List<IdMappingWithAktivitetStatus>> {
        if (ids.isEmpty()) return HashMap()
        val stringIds = ids.stream().map { obj: ArenaId -> obj.id() }.toList()
        val params = MapSqlParameterSource()
            .addValue("arenaIds", stringIds)
        val idList = db.query(
            """
                SELECT ID_MAPPINGER.AKTIVITET_ID, ID_MAPPINGER.EKSTERN_REFERANSE_ID, ID_MAPPINGER.FUNKSJONELL_ID, AKTIVITET.LIVSLOPSTATUS_KODE AS STATUS, HISTORISK_DATO, EKSTERNAKTIVITET.SOURCE 
                FROM ID_MAPPINGER 
                LEFT JOIN AKTIVITET ON ID_MAPPINGER.AKTIVITET_ID = AKTIVITET.AKTIVITET_ID
                LEFT JOIN EKSTERNAKTIVITET ON EKSTERNAKTIVITET.AKTIVITET_ID = AKTIVITET.AKTIVITET_ID
                WHERE ID_MAPPINGER.EKSTERN_REFERANSE_ID in (:arenaIds) AND AKTIVITET.GJELDENDE = 1
                """.trimIndent(), params, rowmapperWithAktivitetStatus
        )
        return idList.groupBy { it.arenaId }
    }

    fun onlyLatestMappings(allMappings: Map<ArenaId, List<IdMappingWithAktivitetStatus>>): Map<ArenaId, IdMappingWithAktivitetStatus> {
        return allMappings.mapValues { (_, second) ->
            second.maxBy { it.historiskDato ?: LocalDateTime.MAX }
        }
    }


    private var rowmapper = RowMapper { rs: ResultSet, _: Int ->
        IdMapping(
            ArenaId(rs.getString("EKSTERN_REFERANSE_ID")),
            rs.getLong("AKTIVITET_ID"),
            UUID.fromString(rs.getString("FUNKSJONELL_ID")),
        )
    }

    private var rowmapperWithAktivitetStatus = RowMapper { rs: ResultSet, _: Int ->
        IdMappingWithAktivitetStatus(
            ArenaId(rs.getString("EKSTERN_REFERANSE_ID")),
            rs.getLong("AKTIVITET_ID"),
            UUID.fromString(rs.getString("FUNKSJONELL_ID")),
            AktivitetStatus.valueOf(rs.getString("STATUS")),
            rs.getTimestamp("HISTORISK_DATO")?.toLocalDateTime(),
            MessageSource.valueOf(rs.getString("SOURCE"))
        )
    }
}
