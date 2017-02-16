package no.nav.fo.veilarbaktivitet.db;

import no.nav.fo.veilarbaktivitet.domain.*;
import org.slf4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static java.util.Optional.ofNullable;
import static no.nav.fo.veilarbaktivitet.db.SQLUtils.hentDato;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.EGENAKTIVITET;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.JOBBSØKING;
import static no.nav.fo.veilarbaktivitet.util.EnumUtils.getName;
import static no.nav.fo.veilarbaktivitet.util.EnumUtils.valueOf;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class AktivitetDAO {

    private static final Logger LOG = getLogger(AktivitetDAO.class);

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private SQLUtils sqlUtils;

    @Inject
    private EndringsLoggDAO endringsLoggDAO;
    //TODO use when update status is in the works

    public List<StillingsSoekAktivitet> hentStillingsAktiviteterForAktorId(String aktorId) {
        return jdbcTemplate.query("SELECT * FROM AKTIVITET A " +
                        "LEFT JOIN STILLINGSSOK S ON A.aktivitet_id = S.aktivitet_id " +
                        "WHERE type = ? AND aktor_id = ?",
                this::mapStillingsAktivitet,
                JOBBSØKING.name(),
                aktorId
        );
    }

    public List<EgenAktivitetData> hentEgenAktiviteterForAktorId(String aktorId) {
        return jdbcTemplate.query("SELECT * FROM AKTIVITET where type = ? AND aktor_id = ?",
                this::mapEgenAktivitet,
                EGENAKTIVITET.name(),
                aktorId
        );
    }

    private AktivitetData mapAktivitet(ResultSet rs) throws SQLException {
        long aktivitetId = rs.getLong("aktivitet_id");
        //TODO vurdere å slå opp alle kommentarer en gang, istede for en gang pr aktivitet
        List<KommentarData> kommentarer = jdbcTemplate.query("SELECT * FROM KOMMENTAR where aktivitet_id = ?", this::mapKommentar, aktivitetId);
        return new AktivitetData()
                .setId(aktivitetId)
                .setAktorId(rs.getString("aktor_id"))
                .setAktivitetType(AktivitetTypeData.valueOf(rs.getString("type")))
                .setFraDato(hentDato(rs, "fra_dato"))
                .setTilDato(hentDato(rs, "til_dato"))
                .setTittel(rs.getString("tittel"))
                .setBeskrivelse(rs.getString("beskrivelse"))
                .setStatus(valueOf(AktivitetStatusData.class, rs.getString("status")))
                .setAvsluttetDato(hentDato(rs, "avsluttet_dato"))
                .setAvsluttetKommentar(rs.getString("avsluttet_kommentar"))
                .setOpprettetDato(hentDato(rs, "opprettet_dato"))
                .setLagtInnAv(valueOf(InnsenderData.class, rs.getString("lagt_inn_av")))
                .setDeleMedNav(rs.getBoolean("dele_med_nav"))
                .setLenke(rs.getString("lenke"))
                .setKommentarer(kommentarer)
                ;
    }

    private KommentarData mapKommentar(ResultSet rs, @SuppressWarnings("unused") int n) throws SQLException {
        return new KommentarData()
                .setKommentar(rs.getString("kommentar"))
                .setOpprettetDato(hentDato(rs, "opprettet_dato"))
                .setOpprettetAv(rs.getString("opprettet_av"))
                ;
    }

    private StillingsSoekAktivitet mapStillingsAktivitet(ResultSet rs, @SuppressWarnings("unused") int n) throws SQLException {
        return new StillingsSoekAktivitet().setAktivitet(mapAktivitet(rs)).setStillingsoek(new StillingsoekData()
                .setStillingsTittel(rs.getString("stillingstittel"))
                .setArbeidsgiver(rs.getString("arbeidsgiver"))
                .setKontaktPerson(rs.getString("kontaktperson"))
                .setStillingsoekEtikett(valueOf(StillingsoekEtikettData.class, rs.getString("etikett")))
        );
    }

    private EgenAktivitetData mapEgenAktivitet(ResultSet rs, @SuppressWarnings("unused") int n) throws SQLException {
        return new EgenAktivitetData().setAktivitet(mapAktivitet(rs));
    }

    @Transactional
    public StillingsSoekAktivitet opprettStillingAktivitet(StillingsSoekAktivitet stillingsSoekAktivitet) {
        opprettAktivitet(stillingsSoekAktivitet.getAktivitet(), JOBBSØKING);
        opprettStillingsSøk(stillingsSoekAktivitet);
        return stillingsSoekAktivitet;
    }

    private void opprettStillingsSøk(StillingsSoekAktivitet stillingsSoekAktivitet) {
        ofNullable(stillingsSoekAktivitet.getStillingsoek()).ifPresent(stillingsoek -> {
            jdbcTemplate.update("INSERT INTO STILLINGSSOK(aktivitet_id,stillingstittel,arbeidsgiver,kontaktperson,etikett) VALUES(?,?,?,?,?)",
                    stillingsSoekAktivitet.getAktivitet().getId(),
                    stillingsoek.getStillingsTittel(),
                    stillingsoek.getArbeidsgiver(),
                    stillingsoek.getKontaktPerson(),
                    getName(stillingsoek.getStillingsoekEtikett())
            );
        });
    }

    public EgenAktivitetData opprettEgenAktivitet(EgenAktivitetData egenAktivitet) {
        opprettAktivitet(egenAktivitet.getAktivitet(), EGENAKTIVITET);
        return egenAktivitet;
    }

    private AktivitetData opprettAktivitet(AktivitetData aktivitet, AktivitetTypeData aktivitetType) {
        long aktivitetId = sqlUtils.nesteFraSekvens("AKTIVITET_ID_SEQ");
        jdbcTemplate.update("INSERT INTO AKTIVITET(aktivitet_id,aktor_id,type,fra_dato,til_dato,tittel,beskrivelse,status,avsluttet_dato,avsluttet_kommentar,opprettet_dato,lagt_inn_av,lenke,dele_med_nav) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                aktivitetId,
                aktivitet.getAktorId(),
                aktivitetType.name(),
                aktivitet.getFraDato(),
                aktivitet.getTilDato(),
                aktivitet.getTittel(),
                aktivitet.getBeskrivelse(),
                getName(aktivitet.getStatus()),
                aktivitet.getAvsluttetDato(),
                aktivitet.getAvsluttetKommentar(),
                aktivitet.getOpprettetDato(),
                getName(aktivitet.getLagtInnAv()),
                aktivitet.getLenke(),
                aktivitet.isDeleMedNav()
        );
        aktivitet.setId(aktivitetId);

        aktivitet.getKommentarer().forEach(k -> jdbcTemplate.update("INSERT INTO KOMMENTAR(aktivitet_id,kommentar,opprettet_av,opprettet_dato) VALUES (?,?,?,?)",
                aktivitetId,
                k.getKommentar(),
                k.getOpprettetAv(),
                k.getOpprettetDato()
        ));

        LOG.info("opprettet {}", aktivitet);
        return aktivitet;
    }

}
