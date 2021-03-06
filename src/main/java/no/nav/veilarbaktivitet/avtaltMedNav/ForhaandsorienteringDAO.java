package no.nav.veilarbaktivitet.avtaltMedNav;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.veilarbaktivitet.avtaltMedNav.varsel.VarselIdHolder;
import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.util.EnumUtils;
import org.slf4j.Logger;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

@Repository
@RequiredArgsConstructor
public class ForhaandsorienteringDAO {
    private final Database database;

    private static final String SELECT_AKTIVITET = "SELECT ID, AKTOR_ID, AKTIVITET_ID, AKTIVITET_VERSJON, ARENAAKTIVITET_ID, TYPE, TEKST, OPPRETTET_DATO, OPPRETTET_AV, LEST_DATO, VARSEL_ID, VARSEL_SKAL_STOPPES, VARSEL_STOPPET " +
            "FROM FORHAANDSORIENTERING ";

    private static final Logger LOG = getLogger(ForhaandsorienteringDAO.class);

    public Forhaandsorientering insert(AvtaltMedNavDTO fhoData, long aktivitetId, Person.AktorId aktorId, String opprettetAv, Date opprettetDato) {
        var id = UUID.randomUUID();
        var fho = fhoData.getForhaandsorientering();
        database.update("INSERT INTO FORHAANDSORIENTERING(ID, AKTOR_ID, AKTIVITET_ID, AKTIVITET_VERSJON, ARENAAKTIVITET_ID, TYPE, TEKST, OPPRETTET_DATO, OPPRETTET_AV, LEST_DATO)" +
                        "VALUES (?,?,?,?,?,?,?,?,?,?)",
                id.toString(),
                aktorId.get(),
                aktivitetId,
                fhoData.getAktivitetVersjon(),
                null,
                fho.getType().name(),
                fho.getTekst(),
                opprettetDato,
                opprettetAv,
                null
        );

        LOG.info("opprettet forhåndsorientering: {} med id: {}", fhoData, id);

        return getById(id.toString());
    }

    public Forhaandsorientering insertForArenaAktivitet(ForhaandsorienteringDTO fho, String arenaAktivitetId, Person.AktorId aktorId, String opprettetAv, Date opprettetDato) {
        var id = UUID.randomUUID();
        database.update("INSERT INTO FORHAANDSORIENTERING(ID, AKTOR_ID, AKTIVITET_ID, AKTIVITET_VERSJON, ARENAAKTIVITET_ID, TYPE, TEKST, OPPRETTET_DATO, OPPRETTET_AV, LEST_DATO)" +
                        "VALUES (?,?,?,?,?,?,?,?,?,?)",
                id.toString(),
                aktorId.get(),
                null,
                null,
                arenaAktivitetId,
                fho.getType().name(),
                fho.getTekst(),
                opprettetDato,
                opprettetAv,
                null
        );

        LOG.info("opprettet forhåndsorientering: {} med id: {}", fho, id);

        return getById(id.toString());
    }

    public void markerSomLest(String id, Date lestDato, Long lestVersjon) {
        // language=sql
        var rows = database
                .update("UPDATE FORHAANDSORIENTERING SET LEST_DATO = ?, LEST_AKTIVITET_VERSJON = ?, VARSEL_SKAL_STOPPES = ? WHERE ID = ?", lestDato, lestVersjon, lestDato, id);
        if (rows != 1) {
            throw new IllegalStateException("Fant ikke forhåndsorienteringen som skulle oppdateres");
        }
    }

    public boolean settVarselFerdig(String forhaandsorienteringId) {
        //language=sql
        return 1 == database.update("UPDATE FORHAANDSORIENTERING SET VARSEL_SKAL_STOPPES = CURRENT_TIMESTAMP WHERE ID = ? AND VARSEL_SKAL_STOPPES is null", forhaandsorienteringId);
    }

    public Forhaandsorientering getById(String id) {
        try {
            return database.queryForObject(SELECT_AKTIVITET + "WHERE ID = ?", ForhaandsorienteringDAO::map,
                    id);
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
            return database.getNamedJdbcTemplate().query(
                    "SELECT * FROM FORHAANDSORIENTERING WHERE id IN (:ider)",
                    parms,
                    ForhaandsorienteringDAO::mapRow);
        }).flatMap(List::stream).collect(Collectors.toList());
    }

    public Forhaandsorientering getFhoForAktivitet(long aktivitetId) {
        try {
            return database.queryForObject(SELECT_AKTIVITET + "WHERE AKTIVITET_ID = ?", ForhaandsorienteringDAO::map,
                    aktivitetId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public Forhaandsorientering getFhoForArenaAktivitet(String aktivitetId) {
        try {
            return database.queryForObject(SELECT_AKTIVITET + "WHERE ARENAAKTIVITET_ID = ?", ForhaandsorienteringDAO::map,
                    aktivitetId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<Forhaandsorientering> getAlleArenaFHO(Person.AktorId aktorId) {
        //language=sql
        return database.query("SELECT * FROM FORHAANDSORIENTERING WHERE ARENAAKTIVITET_ID is not null AND aktor_id = ?", ForhaandsorienteringDAO::map, aktorId.get());
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

    public List<VarselIdHolder> hentVarslerSkalSendes(int limit) {
        return database.getJdbcTemplate().query("" +
                        "select ID, AKTIVITET_ID, ARENAAKTIVITET_ID, AKTOR_ID " +
                        "from FORHAANDSORIENTERING " +
                        "where VARSEL_ID is null " +
                        "   and VARSEL_SKAL_STOPPES is null " +
                        "   and TYPE != ? " +
                        " fetch first ? rows only",
                new BeanPropertyRowMapper<>(VarselIdHolder.class), Type.IKKE_SEND_FORHAANDSORIENTERING.toString(), limit);
    }

    public void markerVarselSomSendt(String id, String varselId) {
        int update = database.getJdbcTemplate().update("" +
                        "update FORHAANDSORIENTERING set VARSEL_ID = ?" +
                        " where ID = ? and VARSEL_ID is null",
                varselId, id);

        if (update != 1L) {
            throw new IllegalStateException("Forhaandsorientering allerede sendt");
        }
    }

    public long setVarselStoppetForIkkeSendt() {
        return database.getJdbcTemplate().update(
                "" +
                        "update FORHAANDSORIENTERING " +
                        "set VARSEL_STOPPET = CURRENT_TIMESTAMP " +
                        "where VARSEL_ID is null " +
                        "   and VARSEL_SKAL_STOPPES is not null " +
                        "   and VARSEL_STOPPET is null "
        );
    }

    public List<String> hentVarslerSomSkalStoppes(int limit) {
        return database.getJdbcTemplate().queryForList("" +
                        "select VARSEL_ID " +
                        "from FORHAANDSORIENTERING " +
                        "where VARSEL_SKAL_STOPPES is not null " +
                        "   and VARSEL_STOPPET is null " +
                        "   and VARSEL_ID is not null" +
                        " fetch first ? rows only",
                String.class, limit);
    }

    public void markerVareslStoppetSomSendt(String varselId) {
        int update = database.getJdbcTemplate().update("" +
                "update FORHAANDSORIENTERING set VARSEL_STOPPET = CURRENT_TIMESTAMP" +
                " where VARSEL_STOPPET is null " +
                "   and VARSEL_ID = ? ", varselId);

        if (update != 1L) {
            throw new IllegalStateException("Forhaandsorentering varsel allerede stoppet");
        }
    }

}
