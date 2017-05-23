package no.nav.fo.veilarbaktivitet.db.dao;

import lombok.val;
import no.nav.apiapp.feil.VersjonsKonflikt;
import no.nav.fo.veilarbaktivitet.db.Database;
import no.nav.fo.veilarbaktivitet.domain.*;
import no.nav.fo.veilarbaktivitet.feed.producer.AktivitetFeedData;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static no.nav.fo.veilarbaktivitet.db.Database.hentDato;
import static no.nav.fo.veilarbaktivitet.util.EnumUtils.getName;
import static no.nav.fo.veilarbaktivitet.util.EnumUtils.valueOf;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class AktivitetDAO {

    private static final Logger LOG = getLogger(AktivitetDAO.class);

    @Inject
    private Database database;

    @Inject
    private EndringsLoggDAO endringsLoggDAO;

    public List<AktivitetFeedData> hentAktiviteterEtterTidspunkt(Date date) {
        return database.query(
                "SELECT " +
                        "AKTIVITET_ID, " +
                        "AKTOR_ID, " +
                        "TYPE, " +
                        "STATUS, " +
                        "FRA_DATO, " +
                        "TIL_DATO, " +
                        "OPPRETTET_DATO, " +
                        "AVTALT " +
                    "FROM AKTIVITET " +
                    "WHERE OPPRETTET_DATO >= ?",
                this::mapAktivitetForFeed,
                date
                );
    }

    public List<AktivitetData> hentAktiviteterForAktorId(String aktorId) {
        return database.query("SELECT * FROM AKTIVITET A " +
                        "LEFT JOIN STILLINGSSOK S ON A.aktivitet_id = S.aktivitet_id " +
                        "LEFT JOIN EGENAKTIVITET E ON A.aktivitet_id = E.aktivitet_id " +
                        "WHERE A.aktor_id = ?",
                this::mapAktivitet,
                aktorId
        );
    }

    private AktivitetData mapAktivitet(ResultSet rs) throws SQLException {
        long aktivitetId = rs.getLong("aktivitet_id");
        val aktivitet = new AktivitetData()
                .setId(aktivitetId)
                .setVersjon(rs.getLong("versjon"))
                .setAktorId(rs.getString("aktor_id"))
                .setAktivitetType(AktivitetTypeData.valueOf(rs.getString("type")))
                .setFraDato(hentDato(rs, "fra_dato"))
                .setTilDato(hentDato(rs, "til_dato"))
                .setTittel(rs.getString("tittel"))
                .setBeskrivelse(rs.getString("beskrivelse"))
                .setStatus(valueOf(AktivitetStatus.class, rs.getString("status")))
                .setAvsluttetKommentar(rs.getString("avsluttet_kommentar"))
                .setOpprettetDato(hentDato(rs, "opprettet_dato"))
                .setLagtInnAv(valueOf(InnsenderData.class, rs.getString("lagt_inn_av")))
                .setAvtalt(rs.getBoolean("avtalt"))
                .setLenke(rs.getString("lenke"));

        if (aktivitet.getAktivitetType() == AktivitetTypeData.EGENAKTIVITET) {
            aktivitet.setEgenAktivitetData(this.mapEgenAktivitet(rs));
        } else if (aktivitet.getAktivitetType() == AktivitetTypeData.JOBBSOEKING) {
            aktivitet.setStillingsSoekAktivitetData(this.mapStillingsAktivitet(rs));
        }

        return aktivitet;
    }

    private AktivitetFeedData mapAktivitetForFeed(ResultSet rs) throws SQLException {
        long aktivitetId = rs.getLong("aktivitet_id");
        val aktivitet = new AktivitetFeedData()
                .setAktivitetId(String.valueOf(aktivitetId))
                .setAktorId(rs.getString("aktor_id"))
                .setAktivitetType(AktivitetTypeData.valueOf(rs.getString("type")))
                .setFraDato( hentDato(rs, "fra_dato"))
                .setTilDato(hentDato(rs, "til_dato"))
                .setStatus(valueOf(AktivitetStatus.class, rs.getString("status")))
                .setOpprettetDato(hentDato(rs, "opprettet_dato"))
                .setAvtalt(rs.getBoolean("avtalt"));
        return aktivitet;
    }


    private StillingsoekAktivitetData mapStillingsAktivitet(ResultSet rs) throws SQLException {
        return new StillingsoekAktivitetData()
                .setStillingsTittel(rs.getString("stillingstittel"))
                .setArbeidsgiver(rs.getString("arbeidsgiver"))
                .setArbeidssted(rs.getString("arbeidssted"))
                .setKontaktPerson(rs.getString("kontaktperson"))
                .setStillingsoekEtikett(valueOf(StillingsoekEtikettData.class, rs.getString("etikett"))
                );
    }

    private EgenAktivitetData mapEgenAktivitet(ResultSet rs) throws SQLException {
        return new EgenAktivitetData()
                .setHensikt(rs.getString("hensikt"))
                .setOppfolging(rs.getString("oppfolging"));
    }


    public long opprettAktivitet(AktivitetData aktivitet) {
        return opprettAktivitet(aktivitet, new Date());
    }

    @Transactional
    long opprettAktivitet(AktivitetData aktivitet, Date opprettetDate) {
        val aktivitetId = insertAktivitet(aktivitet, opprettetDate);
        insertStillingsSoek(aktivitetId, aktivitet.getStillingsSoekAktivitetData());
        insertEgenAktivitet(aktivitetId, aktivitet.getEgenAktivitetData());
        return aktivitetId;
    }

    @Transactional
    public void oppdaterAktivitet(AktivitetData aktivitet) {
        updateAktivitet(aktivitet);
        Optional.ofNullable(aktivitet.getStillingsSoekAktivitetData()).ifPresent(
                stilling -> updateStillingSoek(aktivitet.getId(), stilling)
        );
        Optional.ofNullable(aktivitet.getEgenAktivitetData()).ifPresent(
                egen -> updateEgenAktivitet(aktivitet.getId(), egen)
        );
    }

    private void updateAktivitet(AktivitetData aktivitetData) {
        long versjon = aktivitetData.getVersjon();
        long nesteVersjon = versjon + 1;
        int antallRaderOppdatert = database.update("UPDATE AKTIVITET SET fra_dato = ?, til_dato = ?, tittel = ?, beskrivelse = ?, " +
                        "avsluttet_kommentar = ?, lenke = ?, avtalt = ?, versjon=?" +
                        "WHERE aktivitet_id = ?" +
                        "AND versjon = ?",
                aktivitetData.getFraDato(),
                aktivitetData.getTilDato(),
                aktivitetData.getTittel(),
                aktivitetData.getBeskrivelse(),
                aktivitetData.getAvsluttetKommentar(),
                aktivitetData.getLenke(),
                aktivitetData.isAvtalt(),
                nesteVersjon,
                aktivitetData.getId(),
                versjon
        );
        if (antallRaderOppdatert != 1) {
            throw new VersjonsKonflikt();
        }
    }

    private void updateStillingSoek(long aktivitetId, StillingsoekAktivitetData stillingsSoekAktivitet) {
        database.update("UPDATE STILLINGSSOK SET stillingstittel = ?, arbeidsgiver = ?, arbeidssted = ?, " +
                        "kontaktperson  = ?, etikett = ? " +
                        "WHERE aktivitet_id = ?",
                stillingsSoekAktivitet.getStillingsTittel(),
                stillingsSoekAktivitet.getArbeidsgiver(),
                stillingsSoekAktivitet.getArbeidssted(),
                stillingsSoekAktivitet.getKontaktPerson(),
                getName(stillingsSoekAktivitet.getStillingsoekEtikett()),
                aktivitetId
        );

    }

    private void updateEgenAktivitet(long aktivitetId, EgenAktivitetData egenAktivitetData) {
        database.update("UPDATE EGENAKTIVITET SET hensikt = ?, oppfolging = ? " +
                        "WHERE aktivitet_id = ?",
                egenAktivitetData.getHensikt(),
                egenAktivitetData.getOppfolging(),
                aktivitetId
        );

    }

    private long insertAktivitet(AktivitetData aktivitet, Date opprettetDato) {
        long aktivitetId = database.nesteFraSekvens("AKTIVITET_ID_SEQ");
        database.update("INSERT INTO AKTIVITET(aktivitet_id, aktor_id, type," +
                        "fra_dato, til_dato, tittel, beskrivelse, status," +
                        "avsluttet_kommentar, opprettet_dato, lagt_inn_av, lenke, avtalt) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                aktivitetId,
                aktivitet.getAktorId(),
                aktivitet.getAktivitetType().name(),
                aktivitet.getFraDato(),
                aktivitet.getTilDato(),
                aktivitet.getTittel(),
                aktivitet.getBeskrivelse(),
                getName(aktivitet.getStatus()),
                aktivitet.getAvsluttetKommentar(),
                opprettetDato,
                getName(aktivitet.getLagtInnAv()),
                aktivitet.getLenke(),
                aktivitet.isAvtalt()
        );
        aktivitet.setId(aktivitetId);
        aktivitet.setOpprettetDato(opprettetDato);

        LOG.info("opprettet {}", aktivitet);
        return aktivitetId;
    }

    private void insertStillingsSoek(long aktivitetId, StillingsoekAktivitetData stillingsSoekAktivitet) {
        ofNullable(stillingsSoekAktivitet)
                .ifPresent(stillingsoek -> {
                    database.update("INSERT INTO STILLINGSSOK(aktivitet_id, stillingstittel, arbeidsgiver," +
                                    "arbeidssted, kontaktperson, etikett) VALUES(?,?,?,?,?,?)",
                            aktivitetId,
                            stillingsoek.getStillingsTittel(),
                            stillingsoek.getArbeidsgiver(),
                            stillingsoek.getArbeidssted(),
                            stillingsoek.getKontaktPerson(),
                            getName(stillingsoek.getStillingsoekEtikett())
                    );
                });
    }

    private void insertEgenAktivitet(long aktivitetId, EgenAktivitetData egenAktivitetData) {
        ofNullable(egenAktivitetData)
                .ifPresent(egen -> {
                    database.update("INSERT INTO EGENAKTIVITET(aktivitet_id, hensikt, oppfolging) VALUES(?,?,?)",
                            aktivitetId,
                            egen.getHensikt(),
                            egen.getOppfolging()
                    );
                });
    }


    public int slettAktivitet(long aktivitetId) {

        database.update("DELETE FROM EGENAKTIVITET WHERE aktivitet_id = ?",
                aktivitetId
        );
        database.update("DELETE FROM STILLINGSSOK WHERE aktivitet_id = ?",
                aktivitetId
        );

        endringsLoggDAO.slettEndringslogg(aktivitetId);

        return database.update("DELETE FROM AKTIVITET WHERE aktivitet_id = ?",
                aktivitetId
        );
    }

    public AktivitetData hentAktivitet(long aktivitetId) {
        return database.queryForObject("SELECT * FROM AKTIVITET A " +
                        "LEFT JOIN STILLINGSSOK S ON A.aktivitet_id = S.aktivitet_id " +
                        "LEFT JOIN EGENAKTIVITET E ON A.aktivitet_id = E.aktivitet_id " +
                        "WHERE A.aktivitet_id = ?",
                this::mapAktivitet,
                aktivitetId
        );
    }


    public int endreAktivitetStatus(long aktivitetId, AktivitetStatus status, String avsluttetKommentar) {
        return database.update("UPDATE AKTIVITET SET status = ?, avsluttet_kommentar = ? WHERE aktivitet_id = ?",
                getName(status),
                avsluttetKommentar,
                aktivitetId
        );
    }

    public int endreAktivitetEtikett(long aktivitetId, StillingsoekEtikettData etikett) {
        return database.update("UPDATE STILLINGSSOK SET etikett = ? WHERE aktivitet_id = ?",
                getName(etikett),
                aktivitetId
        );
    }


}
