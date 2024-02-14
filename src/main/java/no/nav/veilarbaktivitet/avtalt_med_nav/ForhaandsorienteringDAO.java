package no.nav.veilarbaktivitet.avtalt_med_nav;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.veilarbaktivitet.arena.model.ArenaId;
import no.nav.veilarbaktivitet.config.database.Database;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.util.EnumUtils;
import org.slf4j.Logger;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

@Repository
@RequiredArgsConstructor
public class ForhaandsorienteringDAO {
    private final NamedParameterJdbcTemplate template;

    private static final String SELECT_FORHAANDSORIENTERING = "SELECT ID, AKTOR_ID, AKTIVITET_ID, AKTIVITET_VERSJON, ARENAAKTIVITET_ID, TYPE, TEKST, OPPRETTET_DATO, OPPRETTET_AV, LEST_DATO, VARSEL_ID, VARSEL_SKAL_STOPPES, VARSEL_STOPPET " +
            "FROM FORHAANDSORIENTERING ";

    private static final Logger LOG = getLogger(ForhaandsorienteringDAO.class);

    public Forhaandsorientering insert(AvtaltMedNavDTO fhoData, long aktivitetId, Person.AktorId aktorId, String opprettetAv, Date opprettetDato) {
        var id = UUID.randomUUID();
        var fho = fhoData.getForhaandsorientering();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id.toString())
                .addValue("aktorId", aktorId.get())
                .addValue("aktivitetId", aktivitetId)
                .addValue("aktivitetVersion", fhoData.getAktivitetVersjon())
                .addValue("arenaAktivitetId", null)
                .addValue("type", fho.getType().name())
                .addValue("tekst", fho.getTekst())
                .addValue("opprettetDato", opprettetDato)
                .addValue("opprettetAv", opprettetAv)
                .addValue("lestDato", null)
                .addValue("brukernotifikasjon", 1);

        // language=sql
        String sql = """
                 INSERT INTO FORHAANDSORIENTERING(ID, AKTOR_ID, AKTIVITET_ID, AKTIVITET_VERSJON, ARENAAKTIVITET_ID, TYPE, TEKST, OPPRETTET_DATO, OPPRETTET_AV, LEST_DATO, BRUKERNOTIFIKASJON)
                    VALUES (
                    :id,
                    :aktorId,
                    :aktivitetId,
                    :aktivitetVersion,
                    :arenaAktivitetId,
                    :type,
                    :tekst,
                    :opprettetDato,
                    :opprettetAv,
                    :lestDato,
                    :brukernotifikasjon)
                """;
        template.update(sql, params);

        LOG.info("opprettet forhåndsorientering: {} med id: {} og aktivitetId: {}", fhoData, id, aktivitetId);

        return getById(id.toString());
    }

    public Forhaandsorientering insertForArenaAktivitet(
            ForhaandsorienteringDTO fho,
            ArenaId arenaAktivitetId,
            Person.AktorId aktorId,
            String opprettetAv,
            Date opprettetDato,
            Optional<Long> tekniskId) {
        var id = UUID.randomUUID();

        MapSqlParameterSource params1 = new MapSqlParameterSource()
                .addValue("arenaAktivitetId", arenaAktivitetId.id());
        String sql1 = "SELECT count(*) FROM FORHAANDSORIENTERING WHERE ARENAAKTIVITET_ID = :arenaAktivitetId";
        Integer currentFhoOnAktivitet = Optional.ofNullable(template.queryForObject(sql1, params1, int.class)).orElseThrow();
        if (currentFhoOnAktivitet > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Aktiviteten har allerede forhåndsorientering");
        }

        var params = new MapSqlParameterSource()
                .addValue("id", id.toString())
                .addValue("aktivitetId", tekniskId.orElse(null))
                .addValue("aktorId", aktorId.get())
                .addValue("arenaAktivitetId", arenaAktivitetId.id())
                .addValue("type", fho.getType().name())
                .addValue("tekst", fho.getTekst())
                .addValue("opprettetDato", opprettetDato)
                .addValue("opprettetAv", opprettetAv);
        // language=sql
        var sql = """
                INSERT INTO FORHAANDSORIENTERING (ID, AKTOR_ID, AKTIVITET_ID, AKTIVITET_VERSJON, ARENAAKTIVITET_ID, TYPE, TEKST, OPPRETTET_DATO, OPPRETTET_AV, LEST_DATO, BRUKERNOTIFIKASJON)
                VALUES (
                    :id,
                    :aktorId,
                    :aktivitetId,
                    null,
                    :arenaAktivitetId,
                    :type,
                    :tekst,
                    :opprettetDato,
                    :opprettetAv,
                    null,
                    1
                )
        """;
        template.update(sql, params);

        LOG.info("opprettet forhåndsorientering: {} med id: {}", fho, id);

        return getById(id.toString());
    }

    public void markerSomLest(String id, Date lestDato, Long lestVersjon) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("lestDato", lestDato)
                .addValue("lestVersjon", lestVersjon);
        // language=sql
        String sql = """
                UPDATE FORHAANDSORIENTERING
                SET LEST_DATO = :lestDato,
                LEST_AKTIVITET_VERSJON = :lestVersjon,
                VARSEL_SKAL_STOPPES = :lestDato
                WHERE ID = :id
                """;
        int rows = template.update(sql, params);
        if (rows != 1) {
            throw new IllegalStateException("Fant ikke forhåndsorienteringen som skulle oppdateres");
        }
    }

    public boolean settVarselFerdig(String forhaandsorienteringId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", forhaandsorienteringId);
        //language=sql
        String sql = """
            UPDATE FORHAANDSORIENTERING SET VARSEL_SKAL_STOPPES = CURRENT_TIMESTAMP WHERE ID = :id AND VARSEL_SKAL_STOPPES is null
            """;
        return 1 == template.update(sql, params);
    }

    public Forhaandsorientering getById(String id) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id);
        String sql = SELECT_FORHAANDSORIENTERING + "WHERE ID = :id";
        try {
            return template.queryForObject(sql, params, ForhaandsorienteringDAO::mapRow);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * Tar en liste med forhåndsorientering-ider og returnerer en liste med forhåndsorienteringer.
     * Oracle har en begrensning på 1000 elementer i en IN-clause, så listen partisjoneres i sublister av maks 1000 ider,
     * og resultatene joines sammen før retur.
     *
     * @param ider Liste av Foraandsorientering.id
     * @return en liste av Forhaandsorienteringer
     */
    public List<Forhaandsorientering> getById(List<String> ider) {
        if (ider.isEmpty()) return Collections.emptyList();

        return Lists.partition(ider, 1000).stream().map(sublist -> {
            SqlParameterSource parms = new MapSqlParameterSource("ider", sublist);
            return template.query(
                    "SELECT * FROM FORHAANDSORIENTERING WHERE id IN (:ider)",
                    parms,
                    ForhaandsorienteringDAO::mapRow);
        }).flatMap(List::stream).toList();
    }

    public Forhaandsorientering getFhoForAktivitet(long aktivitetId) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("aktivitetId", aktivitetId);
            String sql = SELECT_FORHAANDSORIENTERING + "WHERE AKTIVITET_ID = :aktivitetId";
            return template.queryForObject(sql, params, ForhaandsorienteringDAO::mapRow);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public Forhaandsorientering getFhoForArenaAktivitet(ArenaId arenaId) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("arenaId", arenaId.id());
            String sql = SELECT_FORHAANDSORIENTERING + "WHERE ARENAAKTIVITET_ID = :arenaId ORDER BY ID DESC FETCH FIRST 1 ROWS ONLY";
            return template.queryForObject(sql, params, ForhaandsorienteringDAO::mapRow);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<Forhaandsorientering> getAlleArenaFHO(Person.AktorId aktorId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("aktorId", aktorId.get());
        //language=sql
        String sql = "SELECT * FROM FORHAANDSORIENTERING WHERE ARENAAKTIVITET_ID is not null AND aktor_id = :aktorId";
        return template.query(sql, params, ForhaandsorienteringDAO::mapRow);
    }

    private static Forhaandsorientering mapRow(ResultSet rs, int rowNum) throws SQLException {
        return map(rs);
    }

    private static Forhaandsorientering map(ResultSet rs) throws SQLException {
        return Forhaandsorientering.builder()
                .id(rs.getString("ID"))
                .aktorId(AktorId.of(rs.getString("AKTOR_ID")))
                .aktivitetId(rs.getString("AKTIVITET_ID"))
                .arenaAktivitetId(rs.getString("ARENAAKTIVITET_ID"))
                .aktivitetVersjon(rs.getString("AKTIVITET_VERSJON"))
                .type(EnumUtils.valueOf(Type.class, rs.getString("TYPE")))
                .tekst(rs.getString("TEKST"))
                .opprettetDato(Database.hentDato(rs, "OPPRETTET_DATO"))
                .opprettetAv(rs.getString("OPPRETTET_AV"))
                .lestDato(Database.hentDato(rs, "LEST_DATO"))
                .varselId(rs.getString("VARSEL_ID"))
                .varselSkalStoppesDato(Database.hentDato(rs, "VARSEL_SKAL_STOPPES"))
                .varselStoppetDato(Database.hentDato(rs, "VARSEL_STOPPET"))
                .build();
    }


}
