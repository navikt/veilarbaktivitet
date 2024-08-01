package no.nav.veilarbaktivitet.oppfolging.periode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.config.database.Database;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.veilarbdbutil.VeilarbAktivitetSqlParameterSource;
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
        VeilarbAktivitetSqlParameterSource params = new VeilarbAktivitetSqlParameterSource()
                .addValue("aktorId", oppfolgingsperiode.aktorid())
                .addValue("periode", oppfolgingsperiode.oppfolgingsperiodeId().toString())
                .addValue("startTid", oppfolgingsperiode.startTid())
                .addValue("sluttTid", oppfolgingsperiode.sluttTid());

        int harNyerePeriode = jdbc.queryForObject("""
                select count(*) FROM SISTE_OPPFOLGINGSPERIODE
                where AKTORID = :aktorId
                and (STARTDATO > :startTid)
                """, params, Integer.class);
        if (harNyerePeriode == 1) {
            log.info("Fant nyere siste periode enn:{} start:{} slutt:{}", oppfolgingsperiode.oppfolgingsperiodeId(), oppfolgingsperiode.startTid(), oppfolgingsperiode.sluttTid());
            return;
        }

        jdbc.update("""
            insert into SISTE_OPPFOLGINGSPERIODE
            (PERIODE_UUID, AKTORID, STARTDATO, SLUTTDATO)
            VALUES (:periode, :aktorId, :startTid, :sluttTid) 
            ON CONFLICT (aktorid)
            DO UPDATE SET PERIODE_UUID = :periode, STARTDATO = :startTid, SLUTTDATO = :sluttTid
            """, params);

        log.info("opprettet oppfolgingsperiode: {} start: {} slutt: {}", oppfolgingsperiode.oppfolgingsperiodeId(), oppfolgingsperiode.startTid(), oppfolgingsperiode.sluttTid());
    }
}
