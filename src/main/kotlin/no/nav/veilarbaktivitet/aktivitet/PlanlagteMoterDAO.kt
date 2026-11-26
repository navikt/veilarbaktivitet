package no.nav.veilarbaktivitet.aktivitet

import no.nav.veilarbaktivitet.aktivitet.domain.PlanlagtMote
import no.nav.veilarbaktivitet.aktivitet.dto.KanalDTO
import no.nav.veilarbaktivitet.config.database.Database
import no.nav.veilarbaktivitet.motesms.MoteNotifikasjon
import no.nav.veilarbaktivitet.person.Person
import no.nav.veilarbaktivitet.veilarbdbutil.VeilarbAktivitetSqlParameterSource
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Date

@Repository
open class PlanlagteMoterDAO(
    private val jdbc: NamedParameterJdbcTemplate,
) {
    open fun hentPlanlagteMoter(aktorId: Person.AktorId, tidspunkt: Date): List<PlanlagtMote> {
        val params = MapSqlParameterSource()
            .addValue("aktorId", aktorId.get())
            .addValue("tidspunkt", tidspunkt)
            .addValue("avsluttedeStatuser", PlanlagtMote.IKKE_LENGER_PLANLAGT_STATUSER.map { it.name })
        // language=sql
        val sql = """
            SELECT A.AKTIVITET_ID, A.FRA_DATO, A.KONTORSPERRE_ENHET_ID FROM AKTIVITET A
            WHERE A.AKTOR_ID = :aktorId AND A.GJELDENDE = 1 AND A.AKTIVITET_TYPE_KODE = 'MOTE'
            AND A.FRA_DATO > :tidspunkt
            AND A.HISTORISK_DATO IS NULL
            AND A.LIVSLOPSTATUS_KODE NOT IN (:avsluttedeStatuser)
            ORDER BY A.FRA_DATO, A.AKTIVITET_ID
        """.trimIndent()
        return jdbc.queryForStream(sql, params) { rs, _ ->
            PlanlagtMote(
                rs.getLong("AKTIVITET_ID"),
                Database.hentDato(rs, "FRA_DATO"),
                rs.getString("KONTORSPERRE_ENHET_ID"),
            )
        }.use { it.toList() }
    }

    @Transactional
    open fun hentMoterUtenVarsel(fra: Duration, til: Duration, max: Int): List<MoteNotifikasjon> {
        val params = VeilarbAktivitetSqlParameterSource()
            .addValue("fra", ZonedDateTime.now().plus(fra))
            .addValue("til", ZonedDateTime.now().plus(til))
            .addValue("avslutteteAktiviteter", PlanlagtMote.IKKE_LENGER_PLANLAGT_STATUSER.map { it.name })
        // language=sql
        val sql = """
            select a.AKTIVITET_ID as id, a.VERSJON as aktivitet_version, a.AKTOR_ID as aktor_id, a.FRA_DATO as startdato, m.KANAL as kanal
            from AKTIVITET a
            inner join MOTE m on a.AKTIVITET_ID = m.AKTIVITET_ID and a.VERSJON = m.VERSJON
            where GJELDENDE = 1
            and FRA_DATO between :fra and :til
            and AKTIVITET_TYPE_KODE = 'MOTE'
            and HISTORISK_DATO is null
            and LIVSLOPSTATUS_KODE not in (:avslutteteAktiviteter)
            and not exists(select * from GJELDENDE_MOTE_SMS SMS where a.AKTIVITET_ID = SMS.AKTIVITET_ID)
        """.trimIndent()
        return jdbc.query(sql, params) { rs, _ ->
            MoteNotifikasjon(
                rs.getInt("id").toLong(),
                rs.getInt("aktivitet_version").toLong(),
                Person.aktorId(rs.getString("aktor_id")),
                KanalDTO.valueOf(rs.getString("kanal")),
                Database.hentZonedDateTime(rs, "startdato"),
            )
        }
    }
}
