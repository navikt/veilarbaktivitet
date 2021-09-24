package no.nav.veilarbaktivitet.brukernotifikasjon.oppgave;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselStatus;
import no.nav.veilarbaktivitet.aktivitet.base.AktivitetStatus;
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

    List<SkalSendes> hentVarselSomSkalSendes(int maxAntall) {
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("status", VarselStatus.PENDING.name())
                .addValue("finalAktivitetStatus", List.of(AktivitetStatus.FULLFORT.name(), AktivitetStatus.AVBRUTT.name()))
                .addValue("limit", maxAntall);

        return jdbcTemplate
                .query("" +
                                " select ID, BRUKERNOTIFIKASJON_ID, B.AKTIVITET_ID, MELDING, OPPFOLGINGSPERIODE, A.AKTOR_ID" +
                                " from BRUKERNOTIFIKASJON B " +
                                " inner join AKTIVITET A on A.AKTIVITET_ID = B.AKTIVITET_ID and A.VERSJON = B.OPPRETTET_PAA_AKTIVITET_VERSION " +
                                " where STATUS = :status " +
                                " and A.HISTORISK_DATO is null" +
                                " and A.LIVSLOPSTATUS_KODE not in(:finalAktivitetStatus)" +
                                " and A.GJELDENDE = 1 " +
                                " fetch first :limit rows only",
                        parameterSource, rowMapper);
    }

    int avbrytOppgaverForAktiviteterSomIkkeKanEndres() {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("skal_avsluttes", VarselStatus.PENDING.name())
                .addValue("finalAktivitetStatus", List.of(AktivitetStatus.FULLFORT.name(), AktivitetStatus.AVBRUTT.name()))
                .addValue("avbrutStatus", VarselStatus.AVBRUTT.name());

        return jdbcTemplate.update("" +
                        " update BRUKERNOTIFIKASJON B" +
                        " set STATUS = :avbrutStatus " +
                        " where STATUS =:skal_avsluttes " +
                        " and BEKREFTET_SENDT is null" +
                        " and exists( " +
                        "   Select * from AKTIVITET A " +
                        "   where a.AKTIVITET_ID = b.AKTIVITET_ID " +
                        "   and GJELDENDE = 1 " +
                        "   and (HISTORISK_DATO is not null or LIVSLOPSTATUS_KODE in(:finalAktivitetStatus))" +
                        ")",
                param);
    }

    boolean setSendt(long id) {
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("oldStatus", VarselStatus.PENDING.name())
                .addValue("newStatus", VarselStatus.FORSOKT_SENDT.name());

        int update = jdbcTemplate
                .update("update BRUKERNOTIFIKASJON set BEKREFTET_SENDT = CURRENT_TIMESTAMP, STATUS = :newStatus where ID = :id and STATUS = :oldStatus", parameterSource);

        return update == 1;
    }
}
