package no.nav.veilarbaktivitet.brukernotifikasjon.kvitering;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class KviteringDAO {
    private final NamedParameterJdbcTemplate jdbc;

    public void setFeilet(String bestillingsId) {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("brukernotifikasjonId", bestillingsId)
                .addValue("status", VarselStatus.FEILET.toString());
        jdbc.update("update BRUKERNOTIFIKASJON set VARSEL_FEILET = current_timestamp, STATUS = :status where BRUKERNOTIFIKASJON_ID = :brukernotifikasjonId ", param);
    }

    public void setFulfortForGyldige(String bestillingsId) {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("brukernotifikasjonId", bestillingsId)
                .addValue("status", VarselStatus.SENDT_OK.toString());

        jdbc.update("" +
                        " update BRUKERNOTIFIKASJON " +
                        " set" +
                        " BEKREFTET_SENDT = CURRENT_TIMESTAMP, " +
                        " STATUS = :status" +
                        " where VARSEL_FEILET is null " +
                        " and AVSLUTTET is null " +
                        " and BRUKERNOTIFIKASJON_ID = :brukernotifikasjonId"
                , param
        );
    }
}
