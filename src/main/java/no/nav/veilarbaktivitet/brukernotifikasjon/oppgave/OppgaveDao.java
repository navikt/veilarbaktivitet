package no.nav.veilarbaktivitet.brukernotifikasjon.oppgave;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OppgaveDao {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    RowMapper<SkalSendes> rowMapper = (rs, rowNum) ->
            SkalSendes.builder()
                    .aktivitetId(rs.getLong("aktivitet_id"))
                    .aktorId(rs.getString("aktor_id"))
                    .brukernotifikasjonId(rs.getString("brukernotifikasjon_id"))
                    .id(rs.getLong("id"))
                    .melding(rs.getString("melding"))
                    .oppfolgingsperiode(rs.getString("oppfolgingsperiode"))
                    .build();

    public List<SkalSendes> hentVarselSomSkalSendes(int maxAntall) {
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("status", VarselStatus.PENDING.name())
                .addValue("finalAktivitetStatus", List.of(AktivitetStatus.FULLFORT.name(), AktivitetStatus.AVBRUTT.name()))
                .addValue("limit", maxAntall);

        return jdbcTemplate
                .query("" +
                                " select ID, BRUKERNOTIFIKASJON_ID, B.AKTIVITET_ID, MELDING, OPPFOLGINGSPERIODE, A.AKTOR_ID" +
                                " from BRUKERNOTIFIKASJON B " +
                                " inner join AKTIVITET A on A.AKTIVITET_ID = B.AKTIVITET_ID" +
                                " where STATUS = :status " +
                                " and A.HISTORISK_DATO is null" +
                                " and A.LIVSLOPSTATUS_KODE not in(:finalAktivitetStatus)" +
                                " and A.GJELDENDE = 1 " +
                                " fetch first :limit rows only",
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

    public Integer hentAntallUkvitterteVarslerForsoktSendtSisteDognet(int timerForsinkelse) {
        if (timerForsinkelse >= 24) {
            throw new IllegalArgumentException("Vi sjekker kun bestillinger sendt siste 24 timer, så antall timer forsinkelse kan ikke være større enn 24");
        }
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("sekunder", timerForsinkelse * 60 * 60);

        // language=SQL
        String sql = "" +
                " with delay_interval as (" +
                "    select (current_timestamp - FORSOKT_SENDT) as time_diff" +
                "       from BRUKERNOTIFIKASJON" +
                "    where FORSOKT_SENDT is not null" +
                "      and BEKREFTET_SENDT is null" +
                "      and VARSEL_FEILET is null" +
                "      and FORSOKT_SENDT > trunc(sysdate) - 1" +
                "    order by time_diff desc" +
                " )," +
                " delay_seconds as (" +
                " select" +
                "            extract(day from time_diff) * 24 * 60 * 60" +
                "        +        extract(hour from time_diff) * 60 * 60" +
                "        +        extract(minute from time_diff) * 60" +
                "        +        extract(second from time_diff) as seconds" +
                " from delay_interval" +
                " ) select count(seconds) from delay_seconds where seconds > :sekunder";
        return jdbcTemplate.queryForObject(sql, parameterSource, Integer.class);
    }
}
