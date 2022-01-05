package no.nav.veilarbaktivitet.oppfolging.oppfolginsperiode_adder;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AdderDao {
    private final NamedParameterJdbcTemplate template;

    public long oppdaterAktiviteterForPeriode(Person aktorId, ZonedDateTime startDato, ZonedDateTime sluttDato, UUID uuid) {
        return 0;
    }

    public Person.AktorId hentEnBrukerUtenOpfolingsPeriode() {
        String aktorId = template.getJdbcTemplate().queryForObject("""
                SELECT AKTOR_ID from AKTIVITET
                where OPPFOLIGNSPERIODE_UUID is null
                fetch next 1 row ONLY
                """, String.class);

        return Person.aktorId(aktorId);
    }
}
