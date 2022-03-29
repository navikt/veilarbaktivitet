package no.nav.veilarbaktivitet.brukernotifikasjon.oppgave;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselStatus;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OppgaveDao {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    RowMapper<SkalSendes> rowMapper = OppgaveDao::mapRow;

    @SneakyThrows
    private static SkalSendes mapRow(ResultSet rs, int rowNum) {
        return SkalSendes.builder()
                .fnr(Person.fnr(rs.getString("foedselsnummer")))
                .brukernotifikasjonId(rs.getString("brukernotifikasjon_id"))
                .brukernotifikasjonLopeNummer(rs.getLong("id"))
                .melding(rs.getString("melding"))
                .varselType(VarselType.valueOf(rs.getString("type")))
                .oppfolgingsperiode(rs.getString("oppfolgingsperiode"))
                .smsTekst(rs.getString("smstekst"))
                .epostTitel(rs.getString("eposttittel"))
                .epostBody(rs.getString("epostBody"))
                .url(new URL(rs.getString("url")))
                .build();
    }

    public List<SkalSendes> hentVarselSomSkalSendes(int maxAntall) {
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("status", VarselStatus.PENDING.name())
                .addValue("finalAktivitetStatus", List.of(AktivitetStatus.FULLFORT.name(), AktivitetStatus.AVBRUTT.name()))
                .addValue("limit", maxAntall);

        return jdbcTemplate
                .query("""
                                 select ID, BRUKERNOTIFIKASJON_ID, MELDING, OPPFOLGINGSPERIODE, FOEDSELSNUMMER, TYPE, SMSTEKST, EPOSTTITTEL, EPOSTBODY, URL
                                 from BRUKERNOTIFIKASJON
                                 where STATUS = :status
                                 fetch first :limit rows only
                                """,
                        parameterSource, rowMapper);
    }

    public int avbrytIkkeSendteOppgaverForAvslutteteAktiviteter() {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("skal_avsluttes", VarselStatus.PENDING.name())
                .addValue("finalAktivitetStatus", List.of(AktivitetStatus.FULLFORT.name(), AktivitetStatus.AVBRUTT.name()))
                .addValue("avbruttStatus", VarselStatus.AVBRUTT.name());

        return jdbcTemplate.update("" +
                        " update BRUKERNOTIFIKASJON B" +
                        " set STATUS = :avbruttStatus " +
                        " where STATUS =:skal_avsluttes " +
                        " and FORSOKT_SENDT is null" +
                        " and exists( " +
                        "   Select * from AKTIVITET A " +
                        "   where a.AKTIVITET_ID = b.AKTIVITET_ID " +
                        "   and GJELDENDE = 1 " +
                        "   and (HISTORISK_DATO is not null or LIVSLOPSTATUS_KODE in(:finalAktivitetStatus))" +
                        ")",
                param);
    }

    public boolean setSendt(long id) {
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("oldStatus", VarselStatus.PENDING.name())
                .addValue("newStatus", VarselStatus.SENDT.name());

        int update = jdbcTemplate
                .update("update BRUKERNOTIFIKASJON set forsokt_sendt = CURRENT_TIMESTAMP, STATUS = :newStatus where ID = :id and STATUS = :oldStatus", parameterSource);

        return update == 1;
    }

    public int hentAntallUkvitterteVarslerForsoktSendt(long timerForsinkelse) {
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("date", new Date(Instant.now().minusSeconds(60 * 60 * timerForsinkelse).toEpochMilli()));

        // language=SQL
        String sql = "" +
                " select count(*) " +
                " from BRUKERNOTIFIKASJON " +
                " where VARSEL_KVITTERING_STATUS = 'IKKE_SATT' " +
                " and STATUS = 'SENDT' " +
                " and FORSOKT_SENDT < :date ";

        return jdbcTemplate.queryForObject(sql, parameterSource, int.class);
    }
}
