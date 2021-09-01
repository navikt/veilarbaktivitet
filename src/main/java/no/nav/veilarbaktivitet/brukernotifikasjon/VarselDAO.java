package no.nav.veilarbaktivitet.brukernotifikasjon;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.domain.Person;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class VarselDAO {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public void opprettVarsel(
            String varselId,
            long aktivitetId,
            Person.Fnr foedselsnummer,
            UUID oppfolgingsperiode,
            Varseltype type,
            VarselStatus status
    ) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("varsel_id", varselId)
                .addValue("aktivitet_id", aktivitetId)
                .addValue("foedselsnummer", foedselsnummer.get())
                .addValue("oppfolgingsperiode", oppfolgingsperiode.toString())
                .addValue("type", type.name())
                .addValue("status", status.name());
        jdbcTemplate.update("" +
                        " INSERT INTO varsel (varsel_id, aktivitet_id, foedselsnummer, oppfolgingsperiode, varsel_type, varsel_status, sendt) " +
                        " VALUES (:varsel_id, :aktivitet_id, :foedselsnummer, :oppfolgingsperiode, :varsel_type, :varsel_status, CURRENT_TIMESTAMP) ",
                params);
    }
}
