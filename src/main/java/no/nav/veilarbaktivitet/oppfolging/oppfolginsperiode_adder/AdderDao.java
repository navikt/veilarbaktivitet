package no.nav.veilarbaktivitet.oppfolging.oppfolginsperiode_adder;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AdderDao {
    private final NamedParameterJdbcTemplate template;

    public long oppdaterAktiviteterForPeriode(Person aktorId, ZonedDateTime startDato, ZonedDateTime sluttDato, UUID uuid) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("aktorId", aktorId.get())
                .addValue("startDato", startDato)
                .addValue("sluttDato", sluttDato)
                .addValue("oppfolgingsPeriodeId", uuid);

        if (sluttDato != null) {
            return template.update("""
                UPDATE AKTIVITET SET OPPFOLGINGSPERIODE_UUID = :oppfolgingsPeriodeId
                WHERE AKTOR_ID = :aktorId 
                AND OPPRETTET_DATO BETWEEN :startDato AND :sluttDato
                AND OPPFOLGINGSPERIODE_UUID IS NULL
                """, params);
        } else {
            // aktiv (siste) periode
            return template.update("""
                UPDATE AKTIVITET SET OPPFOLGINGSPERIODE_UUID = :oppfolgingsPeriodeId
                WHERE AKTOR_ID = :aktorId 
                AND OPPRETTET_DATO >= :startDato
                AND OPPFOLGINGSPERIODE_UUID IS NULL
                """, params);
        }


    }

    public Person.AktorId hentEnBrukerUtenOpfolingsPeriode() {
        String aktorId = template.getJdbcTemplate().queryForObject("""
                SELECT AKTOR_ID from AKTIVITET
                where OPPFOLGINGSPERIODE_UUID is null
                fetch next 1 row ONLY
                """, String.class);

        return Person.aktorId(aktorId);
    }
}
