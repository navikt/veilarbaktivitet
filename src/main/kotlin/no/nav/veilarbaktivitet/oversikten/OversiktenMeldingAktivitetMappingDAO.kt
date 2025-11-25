package no.nav.veilarbaktivitet.oversikten

import no.nav.common.types.identer.Fnr
import no.nav.veilarbaktivitet.aktivitet.AktivitetId
import no.nav.veilarbaktivitet.person.Person
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.*

@Repository
open class OversiktenMeldingAktivitetMappingDAO(private val template: NamedParameterJdbcTemplate) {

    open fun lagreKoblingMellomOversiktenMeldingOgAktivitet(
        oversiktenMeldingKey: MeldingKey,
        aktivitetId: AktivitetId,
        kategori: OversiktenMelding.Kategori
    ) {
        val sql = """
            insert into oversikten_melding_aktivitet_mapping (oversikten_melding_key, aktivitet_id, kategori)
            values (:oversiktenMeldingKey, :aktivitetId, :kategori::OVERSIKTEN_KATEGORI)
        """.trimIndent()
        val params = MapSqlParameterSource()
            .addValue("oversiktenMeldingKey", oversiktenMeldingKey)
            .addValue("aktivitetId", aktivitetId)
            .addValue("kategori", kategori.name)
        template.update(sql, params)
    }

    open fun hentMeldingKeyForAktivitet(aktivitetId: AktivitetId, kategori: OversiktenMelding.Kategori): UUID? {
        val sql = """
        select oversikten_melding_key 
        from oversikten_melding_aktivitet_mapping
        where aktivitet_id = :aktivitetId and kategori = :kategori::OVERSIKTEN_KATEGORI
    """.trimIndent()

        val params = mapOf("aktivitetId" to aktivitetId, "kategori" to kategori.name)

        return try {
            template.queryForObject(sql, params, rowMapper)
        } catch (e: EmptyResultDataAccessException) {
            null
        }
    }

    fun hentAktivitetsIdForMeldingerSomSkalAvsluttes(fnr: Fnr): List<AktivitetId> {
        val sql = """
            SELECT oma.aktivitet_id
            FROM oversikten_melding_med_metadata omm
                JOIN oversikten_melding_aktivitet_mapping oma ON omm.melding_key = oma.oversikten_melding_key
            WHERE fnr = :fnr
              AND utsending_status = 'SENDT'
            GROUP BY oma.aktivitet_id
            HAVING
                        COUNT(*) FILTER (WHERE operasjon = 'START') = 1
               AND COUNT(*) FILTER (WHERE operasjon = 'STOPP') = 0;
    """.trimIndent()

        val param = MapSqlParameterSource()
            .addValue("fnr", fnr.toString())

        return template.query(sql, param, aktivitetIdRowMapper)
    }

    val aktivitetIdRowMapper = RowMapper { rs: ResultSet, _: Int ->
        rs.getObject("aktivitet_id", AktivitetId::class.java)
    }

    val rowMapper = RowMapper { rs: ResultSet, _: Int ->
        rs.getObject("melding_key", UUID::class.java)
    }
}
