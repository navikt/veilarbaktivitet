package no.nav.veilarbaktivitet.oppfolging.siste_periode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.UUID;

record OppfolingsPeriode(String aktorid, UUID oppfolgingsperiode, ZonedDateTime startTid, ZonedDateTime slutTid) {}

@Repository
@Slf4j
@RequiredArgsConstructor
class SistePeriodeDAO {
    private final NamedParameterJdbcTemplate jdbc;

    void uppsertOppfolingsperide(OppfolingsPeriode oppfolingsPeriode) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("aktorId", oppfolingsPeriode.aktorid())
                .addValue("periode", oppfolingsPeriode.oppfolgingsperiode().toString())
                .addValue("startTid", oppfolingsPeriode.startTid())
                .addValue("sluttTid", oppfolingsPeriode.slutTid());

        int antallOppdatert = jdbc.update("""
                update SISTE_OPPFOLGINGSPEIODE
                set PERIODE_UUID = :periode,
                STARTDATO = :startTid,
                SLUTTDATO = :sluttTid
                where AKTORID = :aktorId
                """, params);
        if (antallOppdatert == 1) {
            log.info("oppdatert oppfolignsperiode for bruker {}", oppfolingsPeriode);
            return;
        }

        jdbc.update("""
                insert into SISTE_OPPFOLGINGSPEIODE
                (PERIODE_UUID, AKTORID, STARTDATO, SLUTTDATO)
                VALUES ( :aktorId, :periode, :startTid, :sluttTid)
                """, params);

        log.info("oppretet oppfolingsperiode {}", oppfolingsPeriode);
    }
}
