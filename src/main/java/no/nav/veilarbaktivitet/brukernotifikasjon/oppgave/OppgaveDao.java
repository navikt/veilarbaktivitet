package no.nav.veilarbaktivitet.brukernotifikasjon.oppgave;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OppgaveDao {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    List<SkalSendes> hentVarselSomSkalSendes(int maxAntall) {
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("status", VarselStatus.PENDING)
                .addValue("limit", maxAntall);


        return jdbcTemplate
                .queryForList("" +
                                " select ID, BRUKERNOTIFIKASJON_ID, B.AKTIVITET_ID, MELDING, OPPFOLGINGSPERIODE, A.AKTOR_ID, A.LIVSLOPSTATUS_KODE, A.HISTORISK_DATO" +
                                " from BRUKERNOTIFIKASJON B " +
                                " inner join AKTIVITET A on A.AKTIVITET_ID = B.AKTIVITET_ID and A.VERSJON = B.OPPRETTET_PAA_AKTIVITET_VERSION " +
                                " where STATUS = :status" +
                                " and A.GJELDENDE = 1 " +
                                " limit :limit",
                        parameterSource, SkalSendes.class);
    }

    boolean oppdaterStatus(long id, VarselStatus fra, VarselStatus til) {
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("old_status", fra)
                .addValue("new_status", til);

        int update = jdbcTemplate
                .update("update BRUKERNOTIFIKASJON set SENDT = CURRENT_TIMESTAMP, STATUS = :newStatus where ID = :id and STATUS = :oldStatus", parameterSource);

        return update == 1;
    }
}
