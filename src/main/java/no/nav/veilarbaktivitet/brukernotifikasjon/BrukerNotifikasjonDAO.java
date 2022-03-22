package no.nav.veilarbaktivitet.brukernotifikasjon;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.brukernotifikasjon.kvitering.VarselKvitteringStatus;
import no.nav.veilarbaktivitet.person.Person;
import oracle.sql.ROWID;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URL;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class BrukerNotifikasjonDAO {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Transactional
    void opprettBrukernotifikasjonPaaAktivitet(//TODO refactor to object
            UUID brukernotifikasjonId,
            long aktivitetId,
            long aktitetVersion,
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
                aktivitetId,
                aktitetVersion,
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


        long brukernotifikasjonDbId = getGeneratedKey(keyHolder);

        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("brukernotifikasjon_id", brukernotifikasjonDbId)
                .addValue("aktivitet_id", aktivitetId)
                .addValue("opprettet_paa_aktivitet_version", aktitetVersion);

        jdbcTemplate.update("""
                insert into AKTIVITET_BRUKERNOTIFIKASJON
                       (  brukernotifikasjon_id,  aktivitet_id,  opprettet_paa_aktivitet_version)
                values ( :brukernotifikasjon_id, :aktivitet_id, :opprettet_paa_aktivitet_version)
                """, params);
    }

    private GeneratedKeyHolder opprettBrukernotifikasjon(
            UUID brukernotifikasjonId,
            long aktivitetId,
            long aktitetVersion,
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
                .addValue("aktivitet_id", aktivitetId)
                .addValue("opprettet_paa_aktivitet_version", aktitetVersion)
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
                        "        ( brukernotifikasjon_id,  aktivitet_id,  opprettet_paa_aktivitet_version,  foedselsnummer,  oppfolgingsperiode,  type,  status,  varsel_kvittering_status, opprettet,          url,  melding,  smsTekst,  epostTittel,  epostBody) " +
                        " VALUES (:brukernotifikasjon_id, :aktivitet_id, :opprettet_paa_aktivitet_version, :foedselsnummer, :oppfolgingsperiode, :type, :status, :varsel_kvittering_status, CURRENT_TIMESTAMP, :url, :melding, :smsTekst, :epostTittel, :epostBody) ",
                params, generatedKeyHolder, new String[]{"ID"});
        return generatedKeyHolder;

    }

    long setDone(long aktivitetId, VarselType varseltype) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("aktivitetId", aktivitetId)
                .addValue("type", varseltype.name());
        //TODO implement avsluttet aktivitesversion?

        return jdbcTemplate.update("" +
                        " update BRUKERNOTIFIKASJON" +
                        " set STATUS = case when STATUS = 'PENDING' then 'AVBRUTT' else 'SKAL_AVSLUTTES' end" +
                        " where AKTIVITET_ID = :aktivitetId " +
                        " and TYPE = :type" +
                        " and STATUS not in ('AVBRUTT', 'SKAL_AVSLUTTES', 'AVSLUTTET')",
                params);
    }


    @SneakyThrows
    public static long getGeneratedKey(KeyHolder keyHolder) {
        Object generatedKey = keyHolder.getKeyAs(Object.class);

        keyHolder.getKeyList().stream()
                .flatMap( it -> it.entrySet().stream().map(entery -> "key: " + entery.getKey() + " valeu: " + entery.getValue()))
                .forEach(it -> log.info(it));

        if (generatedKey == null) {
            throw new DataAccessResourceFailureException("Generated key not present");
        }

        if (generatedKey instanceof BigDecimal key) {
            // Used by H2
            return key.longValue();
        } else if (generatedKey instanceof ROWID key) {
            // Used by Oracle
            return key.longValue();
        } else {
            throw new DataAccessResourceFailureException("Unknown generated key type");
        }
    }

}
