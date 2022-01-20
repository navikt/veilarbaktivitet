package no.nav.veilarbaktivitet.oppfolging.siste_periode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.config.database.Database;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Slf4j
@RequiredArgsConstructor
class SistePeriodeDAO {
    private final NamedParameterJdbcTemplate jdbc;


    private RowMapper<Oppfolgingsperiode> rowmapper= (rs, rowNum) -> new Oppfolgingsperiode(
            rs.getString("AKTORID"),
            Database.hentMaybeUUID(rs, "PERIODE_UUID"),
            Database.hentZonedDateTime(rs, "STARTDATO"),
            Database.hentZonedDateTime(rs, "SLUTTDATO")
    );

    Optional<Oppfolgingsperiode> hentSisteOppfolgingsPeriode(String aktorId) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("aktorId", aktorId);
        try {
            return Optional.of(jdbc.queryForObject(
                    "SELECT * FROM siste_oppfolgingsperiode WHERE aktorid=:aktorId",
                    params,
                    rowmapper));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }


    void uppsertOppfolingsperide(Oppfolgingsperiode oppfolgingsperiode) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("aktorId", oppfolgingsperiode.aktorid())
                .addValue("periode", oppfolgingsperiode.oppfolgingsperiode().toString())
                .addValue("startTid", oppfolgingsperiode.startTid())
                .addValue("sluttTid", oppfolgingsperiode.sluttTid());

        int antallOppdatert = jdbc.update("""
                update SISTE_OPPFOLGINGSPERIODE
                set PERIODE_UUID = :periode,
                STARTDATO = :startTid,
                SLUTTDATO = :sluttTid
                where AKTORID = :aktorId
                """, params);
        if (antallOppdatert == 1) {
            log.info("oppdatert oppfolgignsperiode {}", oppfolgingsperiode);
            return;
        }

        jdbc.update("""
                insert into SISTE_OPPFOLGINGSPERIODE
                (PERIODE_UUID, AKTORID, STARTDATO, SLUTTDATO)
                VALUES (:periode, :aktorId, :startTid, :sluttTid)
                """, params);

        log.info("opprettet oppfolgingsperiode {}", oppfolgingsperiode);
    }
}
