package no.nav.veilarbaktivitet.brukernotifikasjon.avlsutt;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
class AvsluttDao {
    private final NamedParameterJdbcTemplate jdbc;

    List<SkalAvluttes> getOppgaverSomSkalAvbrytes(int maksAntall) {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("status", VarselStatus.SKAL_AVSLUTTES)
                .addValue("limit", maksAntall);
        return jdbc.queryForList("" +
                        " SELECT BRUKERNOTIFIKASJON_ID, AKTOR_ID" +
                        " from BRUKERNOTIFIKASJON B" +
                        " inner join AKTIVITET A on A.AKTIVITET_ID = B.AKTIVITET_ID " +
                        " where STATUS = :status" +
                        " and GJELDENDE = 1" +
                        " limit :limit",
                param,
                SkalAvluttes.class);
    }

    boolean markerOppgaveSomAvbrutt(String brukernotifikasjonsId) {
        MapSqlParameterSource param = new MapSqlParameterSource("notifikasjonsId", brukernotifikasjonsId)
                .addValue("status", VarselStatus.AVSLUTTET);
        int update = jdbc.update("update BRUKERNOTIFIKASJON set AVSLUTTET = CURRENT_TIMESTAMP, STATUS = :status where BRUKERNOTIFIKASJON_ID = :notifikasjonsId", param);
        return update == 1;
    }

}
