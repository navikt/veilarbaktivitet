package no.nav.veilarbaktivitet.oversikten

import no.nav.veilarbaktivitet.aktivitet.AktivitetId
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.stereotype.Repository

@Repository
open class OversiktenMeldingAktivitetMappingDao(private val template: JdbcTemplate) {

    open fun lagreKoblingMellomOversiktenMeldingOgAktivitet(oversiktenMeldingKey: MeldingKey, aktivitetId: AktivitetId, kategori: OversiktenMelding.Kategori) {
        val sql = """
            insert into oversikten_melding_aktivitet_mapping (oversikten_melding_key, aktivitet_id, kategori)
            values (:oversiktenMeldingKey, :aktivitetId, :kategori)
        """.trimIndent()
        val params = MapSqlParameterSource()
            .addValue("oversiktenMeldingKey", oversiktenMeldingKey)
            .addValue("aktivitetId", aktivitetId)
            .addValue("kategori", kategori)
        template.update(sql, params)
    }

}
