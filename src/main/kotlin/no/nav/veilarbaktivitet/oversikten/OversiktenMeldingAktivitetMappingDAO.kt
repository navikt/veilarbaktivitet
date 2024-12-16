package no.nav.veilarbaktivitet.oversikten

import no.nav.veilarbaktivitet.aktivitet.AktivitetId
import no.nav.veilarbaktivitet.veilarbdbutil.VeilarbAktivitetSqlParameterSource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Types

@Repository
open class OversiktenMeldingAktivitetMappingDAO(private val template: NamedParameterJdbcTemplate) {

    open fun lagreKoblingMellomOversiktenMeldingOgAktivitet(oversiktenMeldingKey: MeldingKey, aktivitetId: AktivitetId, kategori: OversiktenMelding.Kategori) {
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

}
