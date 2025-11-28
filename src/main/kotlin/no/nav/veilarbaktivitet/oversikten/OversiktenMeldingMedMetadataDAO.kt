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

    fun hentInfoForMeldingerSomMangletStoppUnderRepublisering(): List<UdeltSamtalereferatUtenMelding> {
        val sql = """
            with per_aktivitet as (
                select
                    ma.aktivitet_id
                from veilarbaktivitet.oversikten_melding_med_metadata om
                         join veilarbaktivitet.oversikten_melding_aktivitet_mapping ma
                              on om.melding_key = ma.oversikten_melding_key
                where ma.aktivitet_id in (
                                          '25808973',
                                          '25784810',
                                          '25968063',
                                          '25851541',
                                          '25982187',
                                          '24471871',
                                          '25918433',
                                          '25857895',
                                          '24342512',
                                          '25627249',
                                          '25994402',
                                          '25925825',
                                          '25801911',
                                          '25891070',
                                          '25899962',
                                          '25917033',
                                          '25905669',
                                          '25926400',
                                          '25991087',
                                          '25779949',
                                          '25134228',
                                          '25652449',
                                          '25768622',
                                          '25075390',
                                          '25979997',
                                          '25944337',
                                          '25867005',
                                          '25741645',
                                          '25982535',
                                          '25939173',
                                          '25994477',
                                          '25989077',
                                          '25872419',
                                          '25597840',
                                          '24449991',
                                          '25562928',
                                          '25875218',
                                          '25808570',
                                          '25939557',
                                          '25909758',
                                          '25942856',
                                          '25586409',
                                          '25574156',
                                          '25845246',
                                          '25889509',
                                          '25667693',
                                          '25597869',
                                          '25733381',
                                          '25798184',
                                          '25930651',
                                          '25798199',
                                          '25952402',
                                          '25798210',
                                          '25991838',
                                          '25574064',
                                          '25924614',
                                          '25969474',
                                          '25715172',
                                          '25969087',
                                          '25574297',
                                          '25900008',
                                          '25986747',
                                          '25967376',
                                          '25873862',
                                          '25549590',
                                          '25933766',
                                          '25675974',
                                          '25653495',
                                          '25882148',
                                          '25992100',
                                          '25574100',
                                          '25725894',
                                          '25942011',
                                          '25994645',
                                          '24696919',
                                          '25699040',
                                          '25914968',
                                          '25944422',
                                          '25941127',
                                          '25574036',
                                          '25975824',
                                          '25872192',
                                          '25810557',
                                          '25834914',
                                          '25529710',
                                          '24566982',
                                          '25777786',
                                          '25938208',
                                          '25682535',
                                          '25597824',
                                          '25674176',
                                          '25776721',
                                          '25675294',
                                          '25850213',
                                          '25574268',
                                          '25561268',
                                          '25918361',
                                          '25598230',
                                          '25901146',
                                          '25967309',
                                          '25574316',
                                          '25772720',
                                          '25247379',
                                          '25805081',
                                          '25994767',
                                          '25813998',
                                          '25929046',
                                          '25574249',
                                          '25796840',
                                          '25032456',
                                          '24672046',
                                          '25953895',
                                          '25574368',
                                          '25777652',
                                          '25994770',
                                          '25867103',
                                          '25739013',
                                          '24921892',
                                          '25279481',
                                          '25597910',
                                          '25715975',
                                          '25764441',
                                          '25792749',
                                          '25970805',
                                          '25945012',
                                          '25574171',
                                          '25743332',
                                          '25521391',
                                          '25215342',
                                          '25174834',
                                          '25966074',
                                          '25918156',
                                          '25574345',
                                          '25826188',
                                          '25941388',
                                          '25954748',
                                          '25931035',
                                          '25618312',
                                          '25574022',
                                          '25981746',
                                          '25446656',
                                          '25864944',
                                          '25888845',
                                          '25701558',
                                          '25965381',
                                          '25984391',
                                          '25905293',
                                          '25500644',
                                          '25926533',
                                          '25994798',
                                          '25917079',
                                          '25939154',
                                          '25920572',
                                          '25382916',
                                          '25574747',
                                          '25943794',
                                          '25737355',
                                          '25994904',
                                          '25862274',
                                          '25427485',
                                          '25926671',
                                          '25796522',
                                          '25976933',
                                          '25967208',
                                          '25604792',
                                          '25920590',
                                          '25796949',
                                          '25903657',
                                          '25487761',
                                          '25994941',
                                          '25616074',
                                          '25965879',
                                          '25661605',
                                          '25994979',
                                          '25711526',
                                          '25727264',
                                          '25717803',
                                          '25918286',
                                          '25814646',
                                          '25914722',
                                          '25258049',
                                          '25994714',
                                          '25257259',
                                          '25792057',
                                          '25995014',
                                          '25444284',
                                          '25954376',
                                          '25790705',
                                          '25171724',
                                          '25930533',
                                          '25883171',
                                          '25516216',
                                          '25807283',
                                          '25836549',
                                          '25914201',
                                          '25093025',
                                          '25660975',
                                          '25824231',
                                          '25124753',
                                          '25994899',
                                          '25580141',
                                          '25936722',
                                          '25768345',
                                          '25502526',
                                          '25704606',
                                          '25749837',
                                          '25824796',
                                          '25920187',
                                          '25370993',
                                          '25980699',
                                          '25984023',
                                          '25058382',
                                          '25781170',
                                          '25864210',
                                          '24746916',
                                          '25659529'
                    )
                group by ma.aktivitet_id
                having
                    sum(case when om.operasjon = 'START' then 1 else 0 end) > 0
                   and
                    sum(case when om.operasjon = 'STOPP' then 1 else 0 end) = 0
            )

            select distinct
                ma.aktivitet_id,
                om.melding_key,
                a.aktor_id
            from veilarbaktivitet.oversikten_melding_med_metadata om
                     join veilarbaktivitet.oversikten_melding_aktivitet_mapping ma
                          on om.melding_key = ma.oversikten_melding_key
                     join per_aktivitet pa
                          on pa.aktivitet_id = ma.aktivitet_id
                     join veilarbaktivitet.aktivitet a
                          on a.aktivitet_id = ma.aktivitet_id
                              and a.gjeldende = 1
            where om.operasjon = 'START'
            order by ma.aktivitet_id, om.melding_key;

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