package no.nav.fo.veilarbaktivitet.db;

import no.nav.fo.veilarbaktivitet.domain.EndringsLogg;
import org.slf4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static no.nav.fo.veilarbaktivitet.db.SQLUtils.hentDato;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class EndringsLoggDAO {

    private static final Logger LOG = getLogger(EndringsLoggDAO.class);

    @Inject
    private JdbcTemplate jdbcTemplate;
    @Inject
    private SQLUtils sqlUtils;

    public List<EndringsLogg> hentEndringdsloggForAktivitetId(String aktivitetId) {
        return jdbcTemplate.query("SELECT * FROM ENDRINGSLOGG WHERE aktivitet_id = ?",
                this::mapEndringsLogg,
                aktivitetId
        );
    }

    private EndringsLogg mapEndringsLogg(ResultSet rs, @SuppressWarnings("unused") int n) throws SQLException {
        return new EndringsLogg()
                .setId(rs.getLong("id"))
                .setEndretDato(hentDato(rs, "endret_dato"))
                .setEndretAv(rs.getString("endret_av"))
                .setEndringsBeskrivelse(rs.getString("endrings_beskrivelse"))
                ;
    }

    protected void opprettEndringsLogg(long aktivitetId, String endretAv, String endringsBeskrivelse) {
        long endringsLoggId = sqlUtils.nesteFraSekvens("ENDINGSLOGG_ID_SEQ");
        jdbcTemplate.update("INSERT INTO ENDRINGSLOGG(id, aktivitet_id, " +
                        "endrings_dato, endret_av, endrings_beskrivelse) " +
                        "VALUES (?,?,?,?,?)",
                endringsLoggId,
                aktivitetId,
                endretAv,
                endringsBeskrivelse);

        LOG.info("opprettet endringslogg with id: {}", endringsLoggId);
    }


}
