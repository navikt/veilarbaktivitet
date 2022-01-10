package no.nav.veilarbaktivitet.oppfolging.oppfolgingsperiode;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;


@Repository
@Slf4j
@RequiredArgsConstructor
public class OppfolgingsperiodeDao {
    private final NamedParameterJdbcTemplate template;

    @Timed
    public long oppdaterAktiviteterForPeriode(Person aktorId, ZonedDateTime startDato, ZonedDateTime sluttDato, UUID uuid) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("aktorId", aktorId.get())
                .addValue("startDato", startDato)
                .addValue("sluttDato", sluttDato)
                .addValue("oppfolgingsperiodeId", uuid.toString());

        if (sluttDato != null) {
            return template.update("""
                    UPDATE AKTIVITET SET OPPFOLGINGSPERIODE_UUID = :oppfolgingsperiodeId
                    WHERE AKTOR_ID = :aktorId
                    AND OPPRETTET_DATO BETWEEN :startDato AND :sluttDato
                    AND OPPFOLGINGSPERIODE_UUID IS NULL
                    """, params);
        } else {
            // aktiv (siste) periode
            return template.update("""
                    UPDATE AKTIVITET SET OPPFOLGINGSPERIODE_UUID = :oppfolgingsperiodeId
                    WHERE AKTOR_ID = :aktorId
                    AND OPPRETTET_DATO >= :startDato
                    AND OPPFOLGINGSPERIODE_UUID IS NULL
                    """, params);
        }
    }

    @Timed
    public List<Person.AktorId> hentBrukereUtenOppfolgingsperiode(int max) {

        MapSqlParameterSource params = new MapSqlParameterSource("maks", max);
        return template.queryForList(
                    """
                    SELECT distinct AKTOR_ID from AKTIVITET
                    where OPPFOLGINGSPERIODE_UUID is null
                    fetch next :maks row ONLY
                    """, params, String.class

            ).stream().map(Person::aktorId).toList();
    }

    @Timed
    public void setUkjentAktorId(Person.AktorId aktorId) {
        MapSqlParameterSource source = new MapSqlParameterSource()
                .addValue("aktorId", aktorId.get());

        template.update("""
                    update  AKTIVITET
                    set OPPFOLGINGSPERIODE_UUID = 'ukjent aktorId'
                    where AKTOR_ID = :aktorId
                    and OPPFOLGINGSPERIODE_UUID is null
                """, source);
    }

    @Timed
    public void setOppfolgingsperiodeTilUkjentForGamleAktiviteterUtenOppfolgingsperiode(Person.AktorId aktorId) {
        MapSqlParameterSource source = new MapSqlParameterSource()
                .addValue("aktorId", aktorId.get());

        int antallOppdatert = template.update("""
                    update  AKTIVITET
                    set OPPFOLGINGSPERIODE_UUID = 'ukjent oppfolginsperiode'
                    where AKTOR_ID = :aktorId
                    and OPPFOLGINGSPERIODE_UUID is null
                    and OPPRETTET_DATO < date '2020-01-01'
                """, source);

        if(antallOppdatert != 0) {
            log.warn("Oppdaterete aktivitere med ukjent oppfolgingsperiode for aktorid {} antall: {}", aktorId.get(),  antallOppdatert);
        }
    }
}
