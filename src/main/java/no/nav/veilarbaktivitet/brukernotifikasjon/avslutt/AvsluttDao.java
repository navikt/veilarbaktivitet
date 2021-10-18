package no.nav.veilarbaktivitet.brukernotifikasjon.avslutt;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
class AvsluttDao {
    private final NamedParameterJdbcTemplate jdbc;
    private final RowMapper<SkalAvluttes> skalAvsluttesMapper = (rs, rowNum) -> new SkalAvluttes(rs.getString("BRUKERNOTIFIKASJON_ID"), rs.getString("AKTOR_ID"), UUID.fromString(rs.getString("OPPFOLGINGSPERIODE")));

    List<SkalAvluttes> getOppgaverSomSkalAvsluttes(int maksAntall) {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("status", VarselStatus.SKAL_AVSLUTTES.name())
                .addValue("limit", maksAntall);
        return jdbc.query("" +
                        " SELECT BRUKERNOTIFIKASJON_ID, AKTOR_ID, OPPFOLGINGSPERIODE" +
                        " from BRUKERNOTIFIKASJON B" +
                        " inner join AKTIVITET A on A.AKTIVITET_ID = B.AKTIVITET_ID " +
                        " where STATUS = :status" +
                        " and GJELDENDE = 1 " +
                        " fetch first :limit rows only",
                param,
                skalAvsluttesMapper);
    }

    int markerAvslutteterAktiviteterSomSkalAvsluttes() {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("skalAvsluttes", VarselStatus.SKAL_AVSLUTTES.name())
                .addValue("avslutteteStatuser", List.of(VarselStatus.SKAL_AVSLUTTES.name(), VarselStatus.AVSLUTTET.name(), VarselStatus.AVBRUTT.name(), VarselStatus.PENDING.name()))
                .addValue("avslutteteAktiviteter", List.of(AktivitetStatus.AVBRUTT.name(), AktivitetStatus.FULLFORT.name()));
        return jdbc.update("" +
                        " update BRUKERNOTIFIKASJON B set STATUS = :skalAvsluttes" +
                        " where STATUS not in (:avslutteteStatuser)" +
                        " and exists(" +
                        "   Select * from AKTIVITET A" +
                        "   where GJELDENDE = 1 " +
                        "   and A.AKTIVITET_ID = B.AKTIVITET_ID " +
                        "   and (HISTORISK_DATO is not null or a.LIVSLOPSTATUS_KODE in(:avslutteteAktiviteter))" +
                        ")",
                param
        );
    }

    boolean markerOppgaveSomAvsluttet(String brukernotifikasjonsId) {
        MapSqlParameterSource param = new MapSqlParameterSource("notifikasjonsId", brukernotifikasjonsId)
                .addValue("status", VarselStatus.AVSLUTTET.name());
        int update = jdbc.update("update BRUKERNOTIFIKASJON set AVSLUTTET = CURRENT_TIMESTAMP, STATUS = :status where BRUKERNOTIFIKASJON_ID = :notifikasjonsId", param);
        return update == 1;
    }

}
