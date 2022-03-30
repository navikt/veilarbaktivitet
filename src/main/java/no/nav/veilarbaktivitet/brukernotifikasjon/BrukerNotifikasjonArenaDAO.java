package no.nav.veilarbaktivitet.brukernotifikasjon;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.brukernotifikasjon.kvitering.VarselKvitteringStatus;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class BrukerNotifikasjonArenaDAO {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Transactional
    public void opprettBrukernotifikasjon(//TODO refactor to object
            UUID brukernotifikasjonId,
            String arenaAktivitetId,
            Person.Fnr foedselsnummer,
            String melding,
            UUID oppfolgingsperiode,
            VarselType type,
            VarselStatus status,
            URL url,
            String epostTitel,
            String epostBody,
            String smsTekst
    ) {
        GeneratedKeyHolder keyHolder = opprettBrukernotifikasjon(
                brukernotifikasjonId,
                foedselsnummer,
                melding,
                oppfolgingsperiode,
                type,
                status,
                url,
                epostTitel,
                epostBody,
                smsTekst
        );


        long brukernotifikasjonDbId = Optional
                .ofNullable(keyHolder.getKeyAs(Object.class))
                .map(Object::toString)
                .map(Long::parseLong)
                .orElseThrow();

        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("brukernotifikasjon_id", brukernotifikasjonDbId)
                .addValue("arena_aktivitet_id", arenaAktivitetId);

        jdbcTemplate.update("""
                insert into ARENA_AKTIVITET_BRUKERNOTIFIKASJON
                       (  brukernotifikasjon_id,  ARENA_AKTIVITET_ID)
                values ( :brukernotifikasjon_id, :arena_aktivitet_id)
                """, params);
    }

    private GeneratedKeyHolder opprettBrukernotifikasjon(
            UUID brukernotifikasjonId,
            Person.Fnr foedselsnummer,
            String melding,
            UUID oppfolgingsperiode,
            VarselType type,
            VarselStatus status,
            URL url,
            String epostTitel,
            String epostBody,
            String smsTekst
    ) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("brukernotifikasjon_id", brukernotifikasjonId.toString())
                .addValue("foedselsnummer", foedselsnummer.get())
                .addValue("oppfolgingsperiode", oppfolgingsperiode.toString())
                .addValue("type", type.name())
                .addValue("url", url.toString())
                .addValue("status", status.name())
                .addValue("varsel_kvittering_status", VarselKvitteringStatus.IKKE_SATT.name())
                .addValue("epostTittel", epostTitel)
                .addValue("epostBody", epostBody)
                .addValue("smsTekst", smsTekst)
                .addValue("melding", melding);


        GeneratedKeyHolder generatedKeyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("" +
                        " INSERT INTO brukernotifikasjon " +
                        "        ( brukernotifikasjon_id,  foedselsnummer,  oppfolgingsperiode,  type,  status,  varsel_kvittering_status, opprettet,          url,  melding,  smsTekst,  epostTittel,  epostBody) " +
                        " VALUES (:brukernotifikasjon_id, :foedselsnummer, :oppfolgingsperiode, :type, :status, :varsel_kvittering_status, CURRENT_TIMESTAMP, :url, :melding, :smsTekst, :epostTittel, :epostBody) ",
                params, generatedKeyHolder, new String[]{"ID"});
        return generatedKeyHolder;

    }

    long setDone(String arenaAktivitetId, VarselType varseltype) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("arenaAktivitetId", arenaAktivitetId)
                .addValue("type", varseltype.name());
        //TODO implement avsluttet aktivitesversion?

        return jdbcTemplate.update("""
            update BRUKERNOTIFIKASJON b
            set STATUS = case when STATUS = 'PENDING' then 'AVBRUTT' else 'SKAL_AVSLUTTES' end
            where exists(select * from ARENA_AKTIVITET_BRUKERNOTIFIKASJON ab
                            where b.id = ab.BRUKERNOTIFIKASJON_ID
                            and ab.ARENA_AKTIVITET_ID = :arenaAktivitetId)
            and TYPE = :type
            and STATUS not in ('AVBRUTT', 'SKAL_AVSLUTTES', 'AVSLUTTET')
            """, params);
    }
}
