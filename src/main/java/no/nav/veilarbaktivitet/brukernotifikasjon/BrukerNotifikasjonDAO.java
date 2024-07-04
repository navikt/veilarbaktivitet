package no.nav.veilarbaktivitet.brukernotifikasjon;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.arena.model.ArenaId;
import no.nav.veilarbaktivitet.brukernotifikasjon.kvittering.VarselKvitteringStatus;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class BrukerNotifikasjonDAO {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public void kobleAktivitetIdTilBrukernotifikasjon(//TODO refactor to object
                                                      long brukernotifikasjonDbId,
                                                      long aktivitetId,
                                                      long aktitetVersion
    ) {

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

    public boolean finnesBrukernotifikasjon(String bestillingsId) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("brukernotifikasjon_id", bestillingsId);
        String sql = """
            SELECT COUNT(*) FROM BRUKERNOTIFIKASJON
            WHERE BRUKERNOTIFIKASJON_ID=:brukernotifikasjon_id
        """;
        int antall = jdbcTemplate.queryForObject(sql, params, int.class);
        return antall > 0;
    }

    public boolean finnesBrukernotifikasjonMedVarselTypeForAktivitet(long aktivitetsId, VarselType varselType) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("aktivitet_id", aktivitetsId)
                .addValue("type", varselType.name());
        String sql = """
            SELECT COUNT(*) FROM BRUKERNOTIFIKASJON BN JOIN AKTIVITET_BRUKERNOTIFIKASJON AB on BN.ID = AB.BRUKERNOTIFIKASJON_ID
            WHERE AB.AKTIVITET_ID = :aktivitet_id
            AND BN.TYPE = :type
        """;
        int antall = jdbcTemplate.queryForObject(sql, params, int.class);
        return antall > 0;
    }

    long opprettBrukernotifikasjon(
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
        jdbcTemplate.update("""
                         INSERT INTO brukernotifikasjon
                                ( brukernotifikasjon_id,  foedselsnummer,  oppfolgingsperiode,  type,  status,  varsel_kvittering_status, opprettet,          url,  melding,  smsTekst,  epostTittel,  epostBody)
                         VALUES (:brukernotifikasjon_id, :foedselsnummer, :oppfolgingsperiode, :type, :status, :varsel_kvittering_status, current_timestamp, :url, :melding, :smsTekst, :epostTittel, :epostBody)
                    """,
                params, generatedKeyHolder, new String[]{"id"});
        return Optional
                .ofNullable(generatedKeyHolder.getKeyAs(Object.class))
                .map(Object::toString)
                .map(Long::parseLong)
                .orElseThrow();

    }

    void kobleArenaAktivitetIdTilBrukernotifikasjon(long brukernotifikasjonDbId, ArenaId arenaAktivitetId) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("brukernotifikasjon_id", brukernotifikasjonDbId)
                .addValue("arena_aktivitet_id", arenaAktivitetId.id());

        jdbcTemplate.update("""
                insert into ARENA_AKTIVITET_BRUKERNOTIFIKASJON
                       (  brukernotifikasjon_id,  ARENA_AKTIVITET_ID)
                values ( :brukernotifikasjon_id, :arena_aktivitet_id)
                """, params);
    }

    long setDone(long aktivitetId, VarselType varseltype) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("aktivitetId", aktivitetId)
                .addValue("type", varseltype.name());
        //TODO implement avsluttet aktivitesversion?

        return jdbcTemplate.update("""
            update BRUKERNOTIFIKASJON b
            set STATUS = case when STATUS = 'PENDING' then 'AVBRUTT' else 'SKAL_AVSLUTTES' end
            where exists(select * from AKTIVITET_BRUKERNOTIFIKASJON ab
                            where b.id = ab.BRUKERNOTIFIKASJON_ID
                            and ab.AKTIVITET_ID = :aktivitetId)
            and TYPE = :type
            and STATUS not in ('AVBRUTT', 'SKAL_AVSLUTTES', 'AVSLUTTET')
            """, params);
    }

    long setDone(ArenaId arenaAktivitetId, VarselType varseltype) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("arenaAktivitetId", arenaAktivitetId.id())
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

    long setDoneGrupperingsID(UUID uuid) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("oppfolgingsperiode", uuid.toString());
        //TODO implement avsluttet aktivitesversion?

        return jdbcTemplate.update("""
                        update BRUKERNOTIFIKASJON
                        set STATUS = case when STATUS = 'PENDING' then 'AVBRUTT' else 'SKAL_AVSLUTTES' end
                        where OPPFOLGINGSPERIODE = :oppfolgingsperiode
                        and STATUS not in ('AVBRUTT', 'SKAL_AVSLUTTES', 'AVSLUTTET')
                        """,
                params);
    }

    public void updateAktivitetIdForArenaBrukernotifikasjon(long aktivitetId, long aktivitetVersjon, ArenaId arenaId) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("arenaId", arenaId.id());
        List<Long> brukernotifikasjonIds = jdbcTemplate.query("""
                            SELECT BRUKERNOTIFIKASJON_ID FROM ARENA_AKTIVITET_BRUKERNOTIFIKASJON
                            WHERE ARENA_AKTIVITET_ID = :arenaId
                        """,
                params, (rs, rowNum) -> rs.getLong("BRUKERNOTIFIKASJON_ID"));



        if(brukernotifikasjonIds.isEmpty()) {
            return;
        } else if (brukernotifikasjonIds.size() > 1) {
            log.error("Flere brukernotifikasjoner for arena-aktivitetid {}", arenaId.id());

        }
        brukernotifikasjonIds
            .forEach(brukernotifikasjonId -> kobleAktivitetIdTilBrukernotifikasjon(brukernotifikasjonId, aktivitetId, aktivitetVersjon));
    }
}
