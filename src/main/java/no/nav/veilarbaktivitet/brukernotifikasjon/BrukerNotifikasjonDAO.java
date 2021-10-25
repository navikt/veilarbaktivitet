package no.nav.veilarbaktivitet.brukernotifikasjon;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class BrukerNotifikasjonDAO {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    void opprettBrukernotifikasjon(
            UUID brukernotifikasjonId,
            long aktivitetId,
            long aktitetVersion,
            Person.Fnr foedselsnummer,
            String melding,
            UUID oppfolgingsperiode,
            VarselType type,
            VarselStatus status,
            VarselFunksjon funksjon
    ) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("brukernotifikasjon_id", brukernotifikasjonId.toString())
                .addValue("aktivitet_id", aktivitetId)
                .addValue("aktivitet_version", aktitetVersion)
                .addValue("foedselsnummer", foedselsnummer.get())
                .addValue("oppfolgingsperiode", oppfolgingsperiode.toString())
                .addValue("type", type.name())
                .addValue("status", status.name())
                .addValue("melding", melding)
                .addValue("funksjon", funksjon.name());
        jdbcTemplate.update("" +
                        " INSERT INTO brukernotifikasjon (brukernotifikasjon_id, aktivitet_id, opprettet_paa_aktivitet_version, foedselsnummer, oppfolgingsperiode, type, status, opprettet, melding, funksjon) " +
                        " VALUES (:brukernotifikasjon_id, :aktivitet_id, :aktivitet_version, :foedselsnummer, :oppfolgingsperiode, :type, :status, CURRENT_TIMESTAMP, :melding, :funksjon) ",
                params);
    }

    private final List<String> skalIkkeAvsluttes = List.of(VarselStatus.SKAL_AVSLUTTES.name(), VarselStatus.AVSLUTTET.name());

    long setDone(long aktivitetId, VarselType varseltype) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("aktivitetId", aktivitetId)
                .addValue("status", VarselStatus.SKAL_AVSLUTTES.name())
                .addValue("type", varseltype.name())
                .addValue("statuses", skalIkkeAvsluttes);

        return jdbcTemplate.update("" +
                        " Update brukernotifikasjon set status=:status where aktivitet_id=:aktivitetId and type = :type and status not in (:statuses)",
                params);
    }

}
