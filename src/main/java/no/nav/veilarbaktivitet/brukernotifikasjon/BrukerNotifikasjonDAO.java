package no.nav.veilarbaktivitet.brukernotifikasjon;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.brukernotifikasjon.kvitering.VarselKvitteringStatus;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

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
            String epostTitel,
            String epostBody,
            String smsTekst
    ) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("brukernotifikasjon_id", brukernotifikasjonId.toString())
                .addValue("aktivitet_id", aktivitetId)
                .addValue("opprettet_paa_aktivitet_version", aktitetVersion)
                .addValue("foedselsnummer", foedselsnummer.get())
                .addValue("oppfolgingsperiode", oppfolgingsperiode.toString())
                .addValue("type", type.name())
                .addValue("status", status.name())
                .addValue("varsel_kvittering_status", VarselKvitteringStatus.IKKE_SATT.name())
                .addValue("epostTittel", epostTitel)
                .addValue("epostBody", epostBody)
                .addValue("smsTekst", smsTekst)
                .addValue("melding", melding);



        jdbcTemplate.update("" +
                        " INSERT INTO brukernotifikasjon " +
                        "        ( brukernotifikasjon_id,  aktivitet_id,  opprettet_paa_aktivitet_version,  foedselsnummer,  oppfolgingsperiode,  type,  status,  varsel_kvittering_status, opprettet,          melding,  smsTekst,  epostTittel,  epostBody) " +
                        " VALUES (:brukernotifikasjon_id, :aktivitet_id, :opprettet_paa_aktivitet_version, :foedselsnummer, :oppfolgingsperiode, :type, :status, :varsel_kvittering_status, CURRENT_TIMESTAMP, :melding, :smsTekst, :epostTittel, :epostBody) ",
                params);
    }

    long setDone(long aktivitetId, VarselType varseltype) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("aktivitetId", aktivitetId)
                .addValue("type", varseltype.name());

        return jdbcTemplate.update("" +
                        " update BRUKERNOTIFIKASJON" +
                        " set STATUS = case when STATUS = 'PENDING' then 'AVBRUTT' else 'SKAL_AVSLUTTES' end" +
                        " where AKTIVITET_ID = :aktivitetId " +
                        " and TYPE = :type" +
                        " and STATUS not in ('AVBRUTT', 'SKAL_AVSLUTTES', 'AVSLUTTET')",
                params);
    }

}
