package no.nav.veilarbaktivitet.oversikten

import no.nav.common.types.identer.Fnr
import no.nav.veilarbaktivitet.config.database.Database
import no.nav.veilarbaktivitet.veilarbdbutil.VeilarbAktivitetSqlParameterSource
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.*

@Repository
open class OversiktenMeldingMedMetadataDAO(
    private val jdbc: NamedParameterJdbcTemplate
) {
    open fun lagre(oversiktenMeldingMedMetadata: OversiktenMeldingMedMetadata) {
        val sql = """ 
            INSERT INTO oversikten_melding_med_metadata (
                    fnr, opprettet, utsending_status, melding, kategori, melding_key, operasjon)
            VALUES ( :fnr, :opprettet, :utsending_status, :melding::json, :kategori, :melding_key, :operasjon)
        """.trimIndent()

        val params = VeilarbAktivitetSqlParameterSource().apply {
            addValue("fnr", oversiktenMeldingMedMetadata.fnr.get())
            addValue("opprettet", oversiktenMeldingMedMetadata.opprettet)
            addValue("utsending_status", oversiktenMeldingMedMetadata.utsendingStatus.name)
            addValue("melding", oversiktenMeldingMedMetadata.meldingSomJson)
            addValue("kategori", oversiktenMeldingMedMetadata.kategori.name)
            addValue("melding_key", oversiktenMeldingMedMetadata.meldingKey)
            addValue("operasjon", oversiktenMeldingMedMetadata.operasjon)
        }

        jdbc.update(sql, params)
    }

    open fun hentAlleSomSkalSendes(): List<OversiktenMeldingMedMetadata> {
        val sql = """
            SELECT * FROM oversikten_melding_med_metadata WHERE utsending_status = 'SKAL_SENDES'
        """.trimIndent()

        return jdbc.query(sql, rowMapper)
    }

    open fun hent(meldingKey: MeldingKey, operasjon: OversiktenMelding.Operasjon): List<OversiktenMeldingMedMetadata> {
        val sql = """
            select * from oversikten_melding_med_metadata
            where melding_key = :melding_key
            and melding->>'operasjon' = :operasjon
        """.trimIndent()

        val params = MapSqlParameterSource().apply {
            addValue("melding_key", meldingKey)
            addValue("operasjon", operasjon.name)
        }

        return jdbc.query(sql, params, rowMapper)
    }

    open fun markerSomSendt(meldingKey: MeldingKey) {
        val sql = """
           UPDATE oversikten_melding_med_metadata
           SET utsending_status = 'SENDT',
           tidspunkt_sendt = now()
           WHERE melding_key = :melding_key
        """.trimIndent()

        val params = VeilarbAktivitetSqlParameterSource().apply {
            addValue("melding_key", meldingKey)
        }

        jdbc.update(sql, params)
    }

    open val rowMapper = RowMapper { rs: ResultSet, rowNum: Int ->
        OversiktenMeldingMedMetadata(
            fnr = Fnr.of(rs.getString("fnr")),
            opprettet = Database.hentZonedDateTime(rs, "opprettet"),
            tidspunktSendt = Database.hentZonedDateTime(rs, "tidspunkt_sendt"),
            utsendingStatus = UtsendingStatus.valueOf(rs.getString("utsending_status")),
            meldingSomJson = rs.getString("melding"),
            kategori = OversiktenMelding.Kategori.valueOf(rs.getString("kategori")),
            meldingKey = UUID.fromString(rs.getString("melding_key")),
            operasjon = OversiktenMelding.Operasjon.valueOf(rs.getString("operasjon")),
        )
    }
}