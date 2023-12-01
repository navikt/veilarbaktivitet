package no.nav.veilarbaktivitet.brukernotifikasjon.varsel

import lombok.SneakyThrows
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonsType
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselStatus
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType
import no.nav.veilarbaktivitet.person.Person
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import org.springframework.stereotype.Service
import java.net.URL
import java.sql.ResultSet
import java.time.Instant
import java.util.*

@Service
class VarselDAO(private val jdbcTemplate: NamedParameterJdbcTemplate) {
    private val rowMapper: RowMapper<SkalSendes> = RowMapper { rs: ResultSet, rowNum: Int -> mapRow(rs, rowNum) }

    internal fun hentVarselSomSkalSendes(maxAntall: Int): List<SkalSendes> {
        val oppgavetyper = VarselType.varslerForBrukernotifikasjonstype(BrukernotifikasjonsType.OPPGAVE).stream()
            .map { obj: VarselType -> obj.name }
            .toList()

        val parameterSource: SqlParameterSource = MapSqlParameterSource()
            .addValue("status", VarselStatus.PENDING.name)
            .addValue("finalAktivitetStatus", listOf(AktivitetStatus.FULLFORT.name, AktivitetStatus.AVBRUTT.name))
            .addValue("oppgavetyper", oppgavetyper)
            .addValue("limit", maxAntall)

        return jdbcTemplate
            .query(
                """
                select ID, BRUKERNOTIFIKASJON_ID, MELDING, OPPFOLGINGSPERIODE, FOEDSELSNUMMER, TYPE, SMSTEKST, EPOSTTITTEL, EPOSTBODY, URL
                from BRUKERNOTIFIKASJON B
                where STATUS = :status
                and not exists(
                   Select * from AKTIVITET A
                   inner join AKTIVITET_BRUKERNOTIFIKASJON AB on A.AKTIVITET_ID = AB.AKTIVITET_ID
                   where AB.BRUKERNOTIFIKASJON_ID = B.ID
                   and A.GJELDENDE = 1
                   and B.TYPE in (:oppgavetyper)
                   and (A.HISTORISK_DATO is not null or A.LIVSLOPSTATUS_KODE in(:finalAktivitetStatus))
                )
                fetch first :limit rows only
            """.trimIndent(),
                parameterSource, rowMapper
            )
    }

    fun avbrytIkkeSendteOppgaverForAvslutteteAktiviteter(): Int {
        val oppgavetype = VarselType.varslerForBrukernotifikasjonstype(BrukernotifikasjonsType.OPPGAVE).stream()
            .map { obj: VarselType -> obj.name }
            .toList()

        val param = MapSqlParameterSource()
            .addValue("oppgavetyper", oppgavetype)
            .addValue("avbruttStatus", VarselStatus.AVBRUTT.name)
            .addValue("skal_avsluttes", VarselStatus.PENDING.name)
            .addValue(
                "finalAktivitetStatus",
                java.util.List.of(AktivitetStatus.FULLFORT.name, AktivitetStatus.AVBRUTT.name)
            )

        return jdbcTemplate.update(
            """
                         update BRUKERNOTIFIKASJON B
                         set STATUS = :avbruttStatus
                         where STATUS =:skal_avsluttes
                         and FORSOKT_SENDT is null
                         and exists(
                           Select * from AKTIVITET A
                           inner join AKTIVITET_BRUKERNOTIFIKASJON AB on A.AKTIVITET_ID = AB.AKTIVITET_ID
                           where AB.BRUKERNOTIFIKASJON_ID = B.ID
                           and A.GJELDENDE = 1
                           and B.TYPE in (:oppgavetyper)
                           and (A.HISTORISK_DATO is not null or A.LIVSLOPSTATUS_KODE in(:finalAktivitetStatus))
                           and A.VERSJON = AB.OPPRETTET_PAA_AKTIVITET_VERSION
                        )
                        """.trimIndent(),
            param
        )
    }

    fun setSendt(id: Long): Boolean {
        val parameterSource: SqlParameterSource = MapSqlParameterSource()
            .addValue("id", id)
            .addValue("oldStatus", VarselStatus.PENDING.name)
            .addValue("newStatus", VarselStatus.SENDT.name)

        val update = jdbcTemplate
            .update(
                "update BRUKERNOTIFIKASJON set forsokt_sendt = CURRENT_TIMESTAMP, STATUS = :newStatus where ID = :id and STATUS = :oldStatus",
                parameterSource
            )

        return update == 1
    }

    fun hentAntallUkvitterteVarslerForsoktSendt(timerForsinkelse: Long): Int {
        val parameterSource: SqlParameterSource = MapSqlParameterSource()
            .addValue("date", Date(Instant.now().minusSeconds(60 * 60 * timerForsinkelse).toEpochMilli()))
        // language=SQL
        val sql = """
                 select count(*)
                 from BRUKERNOTIFIKASJON
                 where VARSEL_KVITTERING_STATUS = 'IKKE_SATT'
                 and STATUS = 'SENDT'
                 and FORSOKT_SENDT < :date
                """.trimIndent()

        return jdbcTemplate.queryForObject(sql, parameterSource, Int::class.javaPrimitiveType)
    }

    companion object {
        @SneakyThrows
        private fun mapRow(rs: ResultSet, rowNum: Int): SkalSendes {
            return SkalSendes(
                fnr = Person.fnr(rs.getString("foedselsnummer")),
                brukernotifikasjonId = rs.getString("brukernotifikasjon_id"),
                brukernotifikasjonLopeNummer = rs.getLong("id"),
                melding = rs.getString("melding"),
                varselType = VarselType.valueOf(rs.getString("type")),
                oppfolgingsperiode = rs.getString("oppfolgingsperiode"),
                smsTekst = rs.getString("smstekst"),
                epostTitel = rs.getString("eposttittel"),
                epostBody = rs.getString("epostBody"),
                url = URL(rs.getString("url"))
            )
        }
    }
}
