package no.nav.fo.veilarbaktivitet.db.dao;

import lombok.val;
import no.nav.apiapp.feil.VersjonsKonflikt;
import no.nav.fo.veilarbaktivitet.db.Database;
import no.nav.fo.veilarbaktivitet.domain.*;
import no.nav.fo.veilarbaktivitet.feed.producer.AktivitetFeedData;
import org.slf4j.Logger;
import org.springframework.dao.DuplicateKeyException;
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

    public List<AktivitetFeedData> hentAktiviteterEtterTidspunkt(Date date) {
        return database.query(
                "SELECT " +
                        "aktivitet_id, aktor_id, type, status, fra_dato, til_dato, opprettet_dato, avtalt " +
                        "FROM aktivitet " +
                        "WHERE opprettet_dato >= ? and gjeldende = true",
                this::mapAktivitetForFeed,
                date
        );
    }

    public List<AktivitetData> hentAktiviteterForAktorId(String aktorId) {
        return database.query("SELECT * FROM AKTIVITET A " +
                        "LEFT JOIN STILLINGSSOK S ON A.aktivitet_id = S.aktivitet_id  and A.versjon = S.versjon " +
                        "LEFT JOIN EGENAKTIVITET E ON A.aktivitet_id = E.aktivitet_id and A.versjon = E.versjon " +
                        "LEFT JOIN SOKEAVTALE SA ON A.aktivitet_id = SA.aktivitet_id and A.versjon = SA.versjon " +
                        "WHERE A.aktor_id = ? and A.gjeldende = true",
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
                .setLenke(rs.getString("lenke"))
                .setTransaksjonsTypeData(
                        valueOf(TransaksjonsTypeData.class,
                                rs.getString("transaksjons_type"))
                );

        if (aktivitet.getAktivitetType() == AktivitetTypeData.EGENAKTIVITET) {
            aktivitet.setEgenAktivitetData(this.mapEgenAktivitet(rs));
        } else if (aktivitet.getAktivitetType() == AktivitetTypeData.JOBBSOEKING) {
            aktivitet.setStillingsSoekAktivitetData(this.mapStillingsAktivitet(rs));
        } else if (aktivitet.getAktivitetType() == AktivitetTypeData.SOKEAVTALE) {
            aktivitet.setSokeAvtaleAktivitetData(this.mapSokeAvtaleAktivitet(rs));
        }

        return aktivitet;
    }

    private AktivitetFeedData mapAktivitetForFeed(ResultSet rs) throws SQLException {
        long aktivitetId = rs.getLong("aktivitet_id");
        val aktivitet = new AktivitetFeedData()
                .setAktivitetId(String.valueOf(aktivitetId))
                .setAktorId(rs.getString("aktor_id"))
                .setAktivitetType(AktivitetTypeData.valueOf(rs.getString("type")))
                .setFraDato(hentDato(rs, "fra_dato"))
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

    private SokeAvtaleAktivitetData mapSokeAvtaleAktivitet(ResultSet rs) throws SQLException {
        return new SokeAvtaleAktivitetData()
                .setAntall(rs.getLong("antall"))
                .setAvtaleOppfolging(rs.getString("avtale_oppfolging"));
    }


    public long opprettAktivitet(AktivitetData aktivitet) {
        return opprettAktivitet(aktivitet, new Date());
    }

    long opprettAktivitet(AktivitetData aktivitet, Date opprettetDate) {
        val aktivitetId = database.nesteFraSekvens("AKTIVITET_ID_SEQ");
        val nyAktivivitet = aktivitet
                .setId(aktivitetId)
                .setVersjon(-1)
                .setOpprettetDato(opprettetDate);

        insertAktivitet(aktivitet);
        return aktivitetId;
    }

    public void oppdaterAktivitet(AktivitetData aktivitet) {

        val oldAktivtet = hentAktivitet(aktivitet.getId());


        // Sjekke versjonskonlfikt her? tror ikke dette vil funke
        // Versjonene vil jo skape problemer mot hverandre....
        // kan prøve å inserte for så å kaste en exception gitt sql constraint exception

        val nyAktivitet = oldAktivtet
                .setFraDato(aktivitet.getFraDato())
                .setTilDato(aktivitet.getTilDato())
                .setTittel(aktivitet.getTittel())
                .setBeskrivelse(aktivitet.getBeskrivelse())
                .setAvsluttetKommentar(aktivitet.getAvsluttetKommentar())
                .setLenke(aktivitet.getLenke())
                .setVersjon(aktivitet.getVersjon())
                .setAvtalt(aktivitet.isAvtalt());

        Optional.ofNullable(oldAktivtet.getStillingsSoekAktivitetData()).ifPresent(
                stilling ->
                        stilling.setArbeidsgiver(aktivitet.getStillingsSoekAktivitetData().getArbeidsgiver())
                                .setArbeidssted(aktivitet.getStillingsSoekAktivitetData().getArbeidssted())
                                .setKontaktPerson(aktivitet.getStillingsSoekAktivitetData().getKontaktPerson())
                                .setStillingsTittel(aktivitet.getStillingsSoekAktivitetData().getStillingsTittel())
        );
        Optional.ofNullable(oldAktivtet.getEgenAktivitetData()).ifPresent(
                egen -> egen
                        .setOppfolging(aktivitet.getEgenAktivitetData().getOppfolging())
                        .setHensikt(aktivitet.getEgenAktivitetData().getHensikt())
        );
        Optional.ofNullable(oldAktivtet.getSokeAvtaleAktivitetData()).ifPresent(
                sokeAvtale -> sokeAvtale
                        .setAntall(aktivitet.getSokeAvtaleAktivitetData().getAntall())
                        .setAvtaleOppfolging(aktivitet.getSokeAvtaleAktivitetData().getAvtaleOppfolging())
        );

        try {
            insertAktivitet(nyAktivitet);
        } catch (DuplicateKeyException e){
            throw new VersjonsKonflikt();
        }
    }

    @Transactional
    private void insertAktivitet(AktivitetData aktivitet) {
        database.update("UPDATE AKTIVITET SET gjeldende = 0 where aktivitet_id = ?", aktivitet.getId());

        val versjon = aktivitet.getVersjon() + 1;
        database.update("INSERT INTO AKTIVITET(aktivitet_id, versjon, aktor_id, type," +
                        "fra_dato, til_dato, tittel, beskrivelse, status," +
                        "avsluttet_kommentar, opprettet_dato, lagt_inn_av, lenke, avtalt, gjeldende, transaksjons_type) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                aktivitet.getId(),
                versjon,
                aktivitet.getAktorId(),
                aktivitet.getAktivitetType().name(),
                aktivitet.getFraDato(),
                aktivitet.getTilDato(),
                aktivitet.getTittel(),
                aktivitet.getBeskrivelse(),
                getName(aktivitet.getStatus()),
                aktivitet.getAvsluttetKommentar(),
                aktivitet.getOpprettetDato(),
                getName(aktivitet.getLagtInnAv()),
                aktivitet.getLenke(),
                aktivitet.isAvtalt(),
                true,
                getName(aktivitet.getTransaksjonsTypeData())
        );

        insertStillingsSoek(aktivitet.getId(), versjon, aktivitet.getStillingsSoekAktivitetData());
        insertEgenAktivitet(aktivitet.getId(), versjon, aktivitet.getEgenAktivitetData());
        insertSokeAvtale(aktivitet.getId(), versjon, aktivitet.getSokeAvtaleAktivitetData());

        LOG.info("opprettet {}", aktivitet);
    }

    private void insertStillingsSoek(long aktivitetId, long versjon, StillingsoekAktivitetData stillingsSoekAktivitet) {
        ofNullable(stillingsSoekAktivitet)
                .ifPresent(stillingsoek -> {
                    database.update("INSERT INTO STILLINGSSOK(aktivitet_id, versjon, stillingstittel," +
                                    "arbeidsgiver, arbeidssted, kontaktperson, etikett) VALUES(?,?,?,?,?,?,?)",
                            aktivitetId,
                            versjon,
                            stillingsoek.getStillingsTittel(),
                            stillingsoek.getArbeidsgiver(),
                            stillingsoek.getArbeidssted(),
                            stillingsoek.getKontaktPerson(),
                            getName(stillingsoek.getStillingsoekEtikett())
                    );
                });
    }

    private void insertEgenAktivitet(long aktivitetId, long versjon, EgenAktivitetData egenAktivitetData) {
        ofNullable(egenAktivitetData)
                .ifPresent(egen -> {
                    database.update("INSERT INTO EGENAKTIVITET(aktivitet_id, versjon, hensikt, oppfolging) " +
                                    "VALUES(?,?,?,?)",
                            aktivitetId,
                            versjon,
                            egen.getHensikt(),
                            egen.getOppfolging()
                    );
                });
    }

    private void insertSokeAvtale(long aktivitetId, long versjon, SokeAvtaleAktivitetData sokeAvtaleAktivitetData) {
        ofNullable(sokeAvtaleAktivitetData)
                .ifPresent(sokeAvtale -> {
                    database.update("INSERT INTO SOKEAVTALE(aktivitet_id, versjon, antall, avtale_oppfolging) " +
                                    "VALUES(?,?,?,?)",
                            aktivitetId,
                            versjon,
                            sokeAvtale.getAntall(),
                            sokeAvtale.getAvtaleOppfolging()
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
        database.update("DELETE FROM SOKEAVTALE WHERE aktivitet_id = ?",
                aktivitetId
        );

        return database.update("DELETE FROM AKTIVITET WHERE aktivitet_id = ?",
                aktivitetId
        );
    }

    public AktivitetData hentAktivitet(long aktivitetId) {
        return database.queryForObject("SELECT * FROM AKTIVITET A " +
                        "LEFT JOIN STILLINGSSOK S ON A.aktivitet_id = S.aktivitet_id AND A.versjon = S.versjon " +
                        "LEFT JOIN EGENAKTIVITET E ON A.aktivitet_id = E.aktivitet_id AND A.versjon = E.versjon " +
                        "LEFT JOIN SOKEAVTALE SA ON A.aktivitet_id = SA.aktivitet_id AND A.versjon = SA.versjon " +
                        "WHERE A.aktivitet_id = ? and gjeldende = true",
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
