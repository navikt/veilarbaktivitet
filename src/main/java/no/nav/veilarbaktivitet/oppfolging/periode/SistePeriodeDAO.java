package no.nav.veilarbaktivitet.oppfolging.periode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.config.database.Database;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Slf4j
@RequiredArgsConstructor
public class SistePeriodeDAO {
    private final NamedParameterJdbcTemplate jdbc;


    private final RowMapper<Oppfolgingsperiode> rowmapper= (rs, rowNum) -> new Oppfolgingsperiode(
            rs.getString("AKTORID"),
            Database.hentMaybeUUID(rs, "PERIODE_UUID"),
            Database.hentZonedDateTime(rs, "STARTDATO"),
            Database.hentZonedDateTime(rs, "SLUTTDATO")
    );

    Optional<Oppfolgingsperiode> hentSisteOppfolgingsPeriode(Person.AktorId aktorId) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("aktorId", aktorId.get());
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
                .addValue("periode", oppfolgingsperiode.oppfolgingsperiodeId().toString())
                .addValue("startTid", oppfolgingsperiode.startTid())
                .addValue("sluttTid", oppfolgingsperiode.sluttTid());

        int antallOppdatert = jdbc.update("""
                update SISTE_OPPFOLGINGSPERIODE
                set PERIODE_UUID = :periode,
                STARTDATO = :startTid,
                SLUTTDATO = :sluttTid
                where AKTORID = :aktorId
                and (STARTDATO < :startTid or PERIODE_UUID = :periode)
                """, params);
        if (antallOppdatert == 1) {
            log.info("oppdatert oppfolgignsperiode {}", oppfolgingsperiode);
            return;
        }

        try {
            jdbc.update("""
                insert into SISTE_OPPFOLGINGSPERIODE
                (PERIODE_UUID, AKTORID, STARTDATO, SLUTTDATO)
                VALUES (:periode, :aktorId, :startTid, :sluttTid)
                """, params);
        } catch (DuplicateKeyException e) {
            log.warn("Insert på duplikat oppfolgingsperiode, ignorerer {}", oppfolgingsperiode.oppfolgingsperiodeId());
        }

        log.info("opprettet oppfolgingsperiode {}", oppfolgingsperiode);
    }
}
