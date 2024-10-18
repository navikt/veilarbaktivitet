package no.nav.veilarbaktivitet.brukernotifikasjon.varsel;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.arena.model.ArenaId;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonsType;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselStatus;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType;
import no.nav.veilarbaktivitet.brukernotifikasjon.kvittering.VarselKvitteringStatus;
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.MinSideBrukernotifikasjonsId;
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.UtgåendeVarsel;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VarselDAO {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    RowMapper<SkalSendes> rowMapper = VarselDAO::mapRow;

    @SneakyThrows
    private static SkalSendes mapRow(ResultSet rs, int rowNum) {
        return SkalSendes.builder()
                .fnr(Person.fnr(rs.getString("foedselsnummer")))
                .varselId(UUID.fromString(rs.getString("brukernotifikasjon_id")))
                .brukernotifikasjonLopeNummer(rs.getLong("id"))
                .melding(rs.getString("melding"))
                .varselType(VarselType.valueOf(rs.getString("type")))
                .oppfolgingsperiode(rs.getString("oppfolgingsperiode"))
                .smsTekst(rs.getString("smstekst"))
                .epostTitel(rs.getString("eposttittel"))
                .epostBody(rs.getString("epostBody"))
                .url(new URL(rs.getString("url")))
                .build();
    }

    public List<SkalSendes> hentVarselSomSkalSendes(int maxAntall) {
        List<String> oppgavetyper = VarselType.varslerForBrukernotifikasjonstype(BrukernotifikasjonsType.OPPGAVE).stream().map(VarselType::name).toList();

        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("status", VarselStatus.PENDING.name())
                .addValue("finalAktivitetStatus", List.of(AktivitetStatus.FULLFORT.name(), AktivitetStatus.AVBRUTT.name()))
                .addValue("oppgavetyper", oppgavetyper)
                .addValue("limit", maxAntall);

        return jdbcTemplate
                .query("""
                                 select ID, BRUKERNOTIFIKASJON_ID, MELDING, OPPFOLGINGSPERIODE, FOEDSELSNUMMER, TYPE, SMSTEKST, EPOSTTITTEL, EPOSTBODY, URL
                                 from BRUKERNOTIFIKASJON B
                                 where STATUS = :status
                                 and not exists(
                                    Select * from AKTIVITET A
                                    inner join AKTIVITET_BRUKERNOTIFIKASJON AB on A.AKTIVITET_ID = AB.AKTIVITET_ID
                                    where AB.BRUKERNOTIFIKASJON_ID = B.ID
                                    and A.GJELDENDE = 1
                                    and B.TYPE in (:oppgavetyper)
                                    and (A.HISTORISK_DATO is not null or A.LIVSLOPSTATUS_KODE in(:finalAktivitetStatus))
                                 )
                                 fetch first :limit rows only
                                """,
                        parameterSource, rowMapper);
    }

    public int avbrytIkkeSendteOppgaverForAvslutteteAktiviteter() {

        List<String> oppgavetype = VarselType.varslerForBrukernotifikasjonstype(BrukernotifikasjonsType.OPPGAVE).stream().map(VarselType::name).toList();

        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("oppgavetyper", oppgavetype)
                .addValue("avbruttStatus", VarselStatus.AVBRUTT.name())
                .addValue("skal_avsluttes", VarselStatus.PENDING.name())
                .addValue("finalAktivitetStatus", List.of(AktivitetStatus.FULLFORT.name(), AktivitetStatus.AVBRUTT.name()));

        return jdbcTemplate.update("""
                         update BRUKERNOTIFIKASJON B
                         set STATUS = :avbruttStatus
                         where STATUS =:skal_avsluttes
                         and FORSOKT_SENDT is null
                         and exists(
                           Select * from AKTIVITET A
                           inner join AKTIVITET_BRUKERNOTIFIKASJON AB on A.AKTIVITET_ID = AB.AKTIVITET_ID
                           where AB.BRUKERNOTIFIKASJON_ID = B.ID
                           and A.GJELDENDE = 1
                           and B.TYPE in (:oppgavetyper)
                           and (A.HISTORISK_DATO is not null or A.LIVSLOPSTATUS_KODE in(:finalAktivitetStatus))
                           and A.VERSJON = AB.OPPRETTET_PAA_AKTIVITET_VERSION
                        )
                        """,
                param);
    }

    public boolean setSendt(long id) {
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("oldStatus", VarselStatus.PENDING.name())
                .addValue("newStatus", VarselStatus.SENDT.name());

        int update = jdbcTemplate
                .update("update BRUKERNOTIFIKASJON set forsokt_sendt = CURRENT_TIMESTAMP, STATUS = :newStatus where ID = :id and STATUS = :oldStatus", parameterSource);

        return update == 1;
    }

    public int hentAntallUkvitterteVarslerForsoktSendt(long timerForsinkelse) {
        SqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("date", new Date(Instant.now().minusSeconds(60 * 60 * timerForsinkelse).toEpochMilli()));

        // language=SQL
        String sql = """
                 select count(*)
                 from BRUKERNOTIFIKASJON
                 where VARSEL_KVITTERING_STATUS = 'IKKE_SATT'
                 and STATUS = 'SENDT'
                 and FORSOKT_SENDT < :date
                """;

        return jdbcTemplate.queryForObject(sql, parameterSource, int.class);
    }

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

    public boolean finnesBrukernotifikasjon(MinSideBrukernotifikasjonsId bestillingsId) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("brukernotifikasjon_id", bestillingsId.getValue().toString());
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

    public long opprettBrukernotifikasjonIOutbox(UtgåendeVarsel utgåendeVarsel) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("brukernotifikasjon_id", utgåendeVarsel.getBrukernotifikasjonId().getValue().toString())
                .addValue("foedselsnummer", utgåendeVarsel.getFoedselsnummer().get())
                .addValue("oppfolgingsperiode", utgåendeVarsel.getOppfolgingsperiode().toString())
                .addValue("type", utgåendeVarsel.getType().name())
                .addValue("url", utgåendeVarsel.getUrl().toString())
                .addValue("status", utgåendeVarsel.getStatus().name())
                .addValue("varsel_kvittering_status", VarselKvitteringStatus.IKKE_SATT.name())
                .addValue("epostTittel", utgåendeVarsel.getEpostTitel())
                .addValue("epostBody", utgåendeVarsel.getEpostBody())
                .addValue("smsTekst", utgåendeVarsel.getSmsTekst())
                .addValue("melding", utgåendeVarsel.getMelding());


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

    public void kobleArenaAktivitetIdTilBrukernotifikasjon(long brukernotifikasjonDbId, ArenaId arenaAktivitetId) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("brukernotifikasjon_id", brukernotifikasjonDbId)
                .addValue("arena_aktivitet_id", arenaAktivitetId.id());

        jdbcTemplate.update("""
                insert into ARENA_AKTIVITET_BRUKERNOTIFIKASJON
                       (  brukernotifikasjon_id,  ARENA_AKTIVITET_ID)
                values ( :brukernotifikasjon_id, :arena_aktivitet_id)
                """, params);
    }

    public long setDone(long aktivitetId, VarselType varseltype) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("aktivitetId", aktivitetId)
                .addValue("type", varseltype.name());
        //TODO implement avsluttet aktivitesversion?

        return jdbcTemplate.update("""
            update BRUKERNOTIFIKASJON b
            set STATUS = case when STATUS = 'PENDING' then 'AVBRUTT' else 'SKAL_AVSLUTTES' end
            from aktivitet_brukernotifikasjon ab
            where ab.aktivitet_id = :aktivitetId
                and ab.brukernotifikasjon_id = b.id
                and b.TYPE = :type
                and b.STATUS not in ('AVBRUTT', 'SKAL_AVSLUTTES', 'AVSLUTTET')
            """, params);
    }

    public long setDone(ArenaId arenaAktivitetId, VarselType varseltype) {
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

    public long setDoneGrupperingsID(UUID uuid) {
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
