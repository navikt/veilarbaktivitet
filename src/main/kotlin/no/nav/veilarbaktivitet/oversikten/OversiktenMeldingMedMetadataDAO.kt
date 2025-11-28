package no.nav.veilarbaktivitet.oversikten

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbaktivitet.config.database.Database
import no.nav.veilarbaktivitet.veilarbdbutil.VeilarbAktivitetSqlParameterSource
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.*

@Repository
open class OversiktenMeldingMedMetadataDAO(
    private val jdbc: NamedParameterJdbcTemplate
) {
    open fun lagre(oversiktenMeldingMedMetadata: OversiktenMeldingMedMetadata): Long {
        val sql = """ 
            INSERT INTO oversikten_melding_med_metadata (
                    fnr, opprettet, utsending_status, melding, kategori, melding_key, operasjon)
            VALUES ( :fnr, :opprettet, :utsending_status::OVERSIKTEN_UTSENDING_STATUS, :melding::json, :kategori::OVERSIKTEN_KATEGORI, :melding_key, :operasjon::OVERSIKTEN_OPERASJON)
        """.trimIndent()

        val params = VeilarbAktivitetSqlParameterSource().apply {
            addValue("fnr", oversiktenMeldingMedMetadata.fnr.get())
            addValue("opprettet", oversiktenMeldingMedMetadata.opprettet)
            addValue("utsending_status", oversiktenMeldingMedMetadata.utsendingStatus.name)
            addValue("melding", oversiktenMeldingMedMetadata.meldingSomJson)
            addValue("kategori", oversiktenMeldingMedMetadata.kategori.name)
            addValue("melding_key", oversiktenMeldingMedMetadata.meldingKey)
            addValue("operasjon", oversiktenMeldingMedMetadata.operasjon.name)
        }

        val keyHolder = GeneratedKeyHolder()
        jdbc.update(sql, params, keyHolder, arrayOf("id"))

        return keyHolder.key?.toLong() ?: throw IllegalStateException("Kunne ikke hente ut nøkkel til lagret melding")
    }

    open fun hentAlleSomSkalSendes(): List<LagretOversiktenMeldingMedMetadata> {
        val sql = """
            SELECT * FROM oversikten_melding_med_metadata WHERE utsending_status = 'SKAL_SENDES'
        """.trimIndent()

        return jdbc.query(sql, rowMapper)
    }

    open fun markerSomSendt(id: Long) {
        val sql = """
           UPDATE oversikten_melding_med_metadata
           SET utsending_status = 'SENDT',
           tidspunkt_sendt = now()
           WHERE id = :id
        """.trimIndent()

        val params = VeilarbAktivitetSqlParameterSource().apply {
            addValue("id", id)
        }

        jdbc.update(sql, params)
    }

    open fun hent(id: Long): LagretOversiktenMeldingMedMetadata? {
        val sql = """
            SELECT * FROM oversikten_melding_med_metadata WHERE id = :id
        """.trimIndent()

        val params = VeilarbAktivitetSqlParameterSource().apply {
            addValue("id", id)
        }

        return jdbc.queryForObject(sql, params, rowMapper)
    }

    open fun hentUdelteSamtalereferatDerViIkkeHarSendtMeldingTilOversikten(): List<UdeltSamtalereferatUtenMelding> {
        val sql = """
            WITH oversikten_meldinger_gruppert_per_key_og_rangert AS (
                SELECT
                    melding_key,
                    operasjon,
                    kategori,
                    ROW_NUMBER() OVER (PARTITION BY melding_key ORDER BY opprettet DESC) AS rank
                FROM oversikten_melding_med_metadata
                WHERE operasjon = 'START'
                  AND kategori = 'UDELT_SAMTALEREFERAT'
            ),
            aktivitet_id_sendt_til_oversikten AS (
                SELECT DISTINCT ma.aktivitet_id
                FROM oversikten_melding_aktivitet_mapping ma
                JOIN oversikten_meldinger_gruppert_per_key_og_rangert me
                    ON ma.oversikten_melding_key = me.melding_key
                WHERE me.rank = 1
            )
            SELECT
                a.aktivitet_id,
                a.aktor_id
            FROM aktivitet a
            LEFT JOIN mote m
                ON a.aktivitet_id = m.aktivitet_id
                AND m.versjon = a.versjon
            WHERE a.gjeldende = 1
              AND (a.aktivitet_type_kode IN ('MOTE', 'SAMTALEREFERAT'))
              AND (m.referat_publisert = 0 AND m.referat IS NOT NULL)
              AND a.historisk_dato IS NULL
              AND a.fra_dato < NOW()
              AND a.aktivitet_id NOT IN (SELECT aktivitet_id FROM aktivitet_id_sendt_til_oversikten)
            ORDER BY a.aktivitet_id;
        """.trimIndent()

        return jdbc.query(sql) { rs: ResultSet, rowNum: Int ->
            UdeltSamtalereferatUtenMelding(AktorId.of(rs.getString("aktor_id")), rs.getLong("aktivitet_id"))
        }
    }

    open fun hentAlleUdelteSamtalereferaterIÅpenPeriode(): List<UdeltSamtalereferatUtenMelding> {
        val sql = """
            SELECT
                a.aktivitet_id,
                a.aktor_id
            FROM aktivitet a
            LEFT JOIN mote m
            on m.aktivitet_id = a.aktivitet_id and m.versjon = a.versjon
            where m.referat is not null
              and m.referat_publisert = 0
              and a.gjeldende =  1 
              and a.historisk_dato is null 

        """.trimIndent()

        return jdbc.query(sql) { rs: ResultSet, rowNum: Int ->
            UdeltSamtalereferatUtenMelding(AktorId.of(rs.getString("aktor_id")), rs.getLong("aktivitet_id"))
        }
    }

    open fun hentAlleUdelteSamtalereferaterIAvbruttAktivitet(): List<UdeltSamtalereferatUtenMelding> {
        val sql = """
            SELECT
                a.aktivitet_id,
                a.aktor_id
            FROM aktivitet a
                     LEFT JOIN mote m
                               on m.aktivitet_id = a.aktivitet_id and m.versjon = a.versjon
            where m.referat is not null
              and m.referat_publisert = 0
              and a.gjeldende =  1
            and a.livslopstatus_kode = 'AVBRUTT'
              and a.historisk_dato is null

        """.trimIndent()

        return jdbc.query(sql) { rs: ResultSet, rowNum: Int ->
            UdeltSamtalereferatUtenMelding(AktorId.of(rs.getString("aktor_id")), rs.getLong("aktivitet_id"))
        }
    }


    data class UdeltSamtalereferatUtenMelding(
        val aktorId: AktorId,
        val aktivitetId: Long,
    )


    open val rowMapper = RowMapper { rs: ResultSet, rowNum: Int ->
        LagretOversiktenMeldingMedMetadata(
            id = rs.getLong("id"),
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