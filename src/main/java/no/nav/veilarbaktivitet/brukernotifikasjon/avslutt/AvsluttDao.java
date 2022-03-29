package no.nav.veilarbaktivitet.brukernotifikasjon.avslutt;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselStatus;
import no.nav.veilarbaktivitet.person.Person;
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
    private final RowMapper<SkalAvluttes> skalAvsluttesMapper = (rs, rowNum) -> new SkalAvluttes(rs.getString("BRUKERNOTIFIKASJON_ID"), Person.fnr(rs.getString("FOEDSELSNUMMER")), UUID.fromString(rs.getString("OPPFOLGINGSPERIODE")));

    List<SkalAvluttes> getOppgaverSomSkalAvsluttes(int maksAntall) {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("status", VarselStatus.SKAL_AVSLUTTES.name())
                .addValue("limit", maksAntall);
        return jdbc.query("" +
                        " SELECT BRUKERNOTIFIKASJON_ID, FOEDSELSNUMMER, OPPFOLGINGSPERIODE" +
                        " from BRUKERNOTIFIKASJON B" +
                        " where STATUS = :status" +
                        " fetch first :limit rows only",
                param,
                skalAvsluttesMapper);
    }

    // TODO: Skriv om til å bruke aktivitet-kafka-topic for å avslutte
    int markerAvslutteterAktiviteterSomSkalAvsluttes() {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("skalAvsluttes", VarselStatus.SKAL_AVSLUTTES.name())
                .addValue("avsluttedeStatuser", List.of(VarselStatus.SKAL_AVSLUTTES.name(), VarselStatus.AVSLUTTET.name(), VarselStatus.AVBRUTT.name(), VarselStatus.PENDING.name()))
                .addValue("avsluttedeAktiviteter", List.of(AktivitetStatus.AVBRUTT.name(), AktivitetStatus.FULLFORT.name()));
        return jdbc.update("""
                         update BRUKERNOTIFIKASJON B set STATUS = :skalAvsluttes
                         where STATUS not in (:avsluttedeStatuser)
                         and exists(
                           Select * from AKTIVITET A
                           inner join AKTIVITET_BRUKERNOTIFIKASJON AB on A.AKTIVITET_ID = AB.AKTIVITET_ID
                           where GJELDENDE = 1
                           and A.AKTIVITET_ID = AB.AKTIVITET_ID
                           and B.ID = AB.BRUKERNOTIFIKASJON_ID
                           and a.LIVSLOPSTATUS_KODE in(:avsluttedeAktiviteter)
                        )
                """, param);
    }


    //TODO skal slettes
    public int avsluttIkkeSendteOppgaver() {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("skal_avsluttes", VarselStatus.SKAL_AVSLUTTES.name())
                .addValue("avbrutStatus", VarselStatus.AVSLUTTET.name());
        return jdbc.update("" +
                        "update BRUKERNOTIFIKASJON set STATUS = :avbrutStatus where STATUS =:skal_avsluttes and FORSOKT_SENDT is null",
                param);
    }

    boolean markerOppgaveSomAvsluttet(String brukernotifikasjonsId) {
        MapSqlParameterSource param = new MapSqlParameterSource("notifikasjonsId", brukernotifikasjonsId)
                .addValue("status", VarselStatus.AVSLUTTET.name());
        int update = jdbc.update("update BRUKERNOTIFIKASJON set AVSLUTTET = CURRENT_TIMESTAMP, STATUS = :status where BRUKERNOTIFIKASJON_ID = :notifikasjonsId", param);
        return update == 1;
    }

}
