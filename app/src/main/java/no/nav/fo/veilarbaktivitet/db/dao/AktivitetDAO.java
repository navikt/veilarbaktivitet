package no.nav.fo.veilarbaktivitet.db.dao;

import lombok.val;
import no.nav.fo.veilarbaktivitet.db.Database;
import no.nav.fo.veilarbaktivitet.domain.*;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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
        //TODO vurdere å slå opp alle kommentarer en gang, istede for en gang pr aktivitet
        List<KommentarData> kommentarer = database.query("SELECT * FROM KOMMENTAR WHERE aktivitet_id = ?", this::mapKommentar, aktivitetId);
        val aktivitet = new AktivitetData()
                .setId(aktivitetId)
                .setAktorId(rs.getString("aktor_id"))
                .setAktivitetType(AktivitetTypeData.valueOf(rs.getString("type")))
                .setFraDato(hentDato(rs, "fra_dato"))
                .setTilDato(hentDato(rs, "til_dato"))
                .setTittel(rs.getString("tittel"))
                .setBeskrivelse(rs.getString("beskrivelse"))
                .setStatus(valueOf(AktivitetStatus.class, rs.getString("status")))
                .setAvsluttetDato(hentDato(rs, "avsluttet_dato"))
                .setAvsluttetKommentar(rs.getString("avsluttet_kommentar"))
                .setOpprettetDato(hentDato(rs, "opprettet_dato"))
                .setLagtInnAv(valueOf(InnsenderData.class, rs.getString("lagt_inn_av")))
                .setDeleMedNav(rs.getBoolean("dele_med_nav"))
                .setLenke(rs.getString("lenke"))
                .setKommentarer(kommentarer);

        if (aktivitet.getAktivitetType() == AktivitetTypeData.EGENAKTIVITET) {
            aktivitet.setEgenAktivitetData(this.mapEgenAktivitet(rs));
        } else if (aktivitet.getAktivitetType() == AktivitetTypeData.JOBBSOEKING) {
            aktivitet.setStillingsSoekAktivitetData(this.mapStillingsAktivitet(rs));
        }

        return aktivitet;
    }


    private KommentarData mapKommentar(ResultSet rs) throws SQLException {
        return new KommentarData()
                .setKommentar(rs.getString("kommentar"))
                .setOpprettetDato(hentDato(rs, "opprettet_dato"))
                .setOpprettetAv(rs.getString("opprettet_av"))
                ;
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
                .setType(valueOf(EgenAktivitetTypeData.class, rs.getString("egen_type")));
    }


    @Transactional
    public AktivitetData opprettAktivitet(AktivitetData aktivitet) {
        //TODO HUGE ISSUE: keep stuff immutable

        val lagretAktivitet = insertAktivitet(aktivitet);
        val lagretStillingSoek = insertStillingsSoek(lagretAktivitet.getId(), aktivitet.getStillingsSoekAktivitetData());
        val lagretEgenAktivitet = insertEgenAktivitet(lagretAktivitet.getId(), aktivitet.getEgenAktivitetData());

        lagretAktivitet.setStillingsSoekAktivitetData(lagretStillingSoek);
        lagretAktivitet.setEgenAktivitetData(lagretEgenAktivitet);

        return lagretAktivitet;
    }


    private AktivitetData insertAktivitet(AktivitetData aktivitet) {
        long aktivitetId = database.nesteFraSekvens("AKTIVITET_ID_SEQ");
        val opprettetDato = new Date();
        database.update("INSERT INTO AKTIVITET(aktivitet_id,aktor_id,type," +
                        "fra_dato,til_dato,tittel,beskrivelse,status,avsluttet_dato," +
                        "avsluttet_kommentar,opprettet_dato,lagt_inn_av,lenke,dele_med_nav) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                aktivitetId,
                aktivitet.getAktorId(),
                aktivitet.getAktivitetType().name(),
                aktivitet.getFraDato(),
                aktivitet.getTilDato(),
                aktivitet.getTittel(),
                aktivitet.getBeskrivelse(),
                getName(aktivitet.getStatus()),
                aktivitet.getAvsluttetDato(),
                aktivitet.getAvsluttetKommentar(),
                opprettetDato,
                getName(aktivitet.getLagtInnAv()),
                aktivitet.getLenke(),
                aktivitet.isDeleMedNav()
        );
        aktivitet.setId(aktivitetId);
        aktivitet.setOpprettetDato(opprettetDato);

        val kommentarer = insertKommentarer(aktivitetId, aktivitet.getKommentarer());
        aktivitet.setKommentarer(kommentarer);

        LOG.info("opprettet {}", aktivitet);
        return aktivitet;
    }

    private List<KommentarData> insertKommentarer(long aktivitetId, List<KommentarData> kommentarer) {
        return kommentarer.stream()
                .map(k -> insertKommentar(aktivitetId, k))
                .collect(Collectors.toList());
    }

    private KommentarData insertKommentar(long aktivitetId, KommentarData kommentar) {
        database.update("INSERT INTO KOMMENTAR(aktivitet_id, kommentar, opprettet_av, opprettet_dato) " +
                        "VALUES (?,?,?,?)",
                aktivitetId,
                kommentar.getKommentar(),
                kommentar.getOpprettetAv(),
                kommentar.getOpprettetDato()
        );
        return kommentar; //Todo set date and such
    }

    private StillingsoekAktivitetData insertStillingsSoek(long aktivitetId, StillingsoekAktivitetData stillingsSoekAktivitet) {
        return ofNullable(stillingsSoekAktivitet)
                .map(stillingsoek -> {
                    database.update("INSERT INTO STILLINGSSOK(aktivitet_id, stillingstittel, arbeidsgiver," +
                                    "arbeidssted, kontaktperson, etikett) VALUES(?,?,?,?,?,?)",
                            aktivitetId,
                            stillingsoek.getStillingsTittel(),
                            stillingsoek.getArbeidsgiver(),
                            stillingsoek.getArbeidssted(),
                            stillingsoek.getKontaktPerson(),
                            getName(stillingsoek.getStillingsoekEtikett())
                    );
                    return stillingsoek;
                }).orElse(null);
    }

    private EgenAktivitetData insertEgenAktivitet(long aktivitetId, EgenAktivitetData egenAktivitetData) {
        return ofNullable(egenAktivitetData)
                .map(egen -> {
                    database.update("INSERT INTO EGENAKTIVITET(aktivitet_id, hensikt, egen_type) VALUES(?,?,?)",
                            aktivitetId,
                            egen.getHensikt(),
                            getName(egen.getType())
                    );
                    return egen;
                }).orElse(null);
    }


    public int slettAktivitet(long aktivitetId) {

        database.update("DELETE FROM KOMMENTAR WHERE aktivitet_id = ?",
                aktivitetId
        );
        database.update("DELETE FROM EGENAKTIVITET WHERE aktivitet_id = ?",
                aktivitetId
        );
        database.update("DELETE FROM STILLINGSSOK WHERE aktivitet_id = ?",
                aktivitetId
        );
        database.update("DELETE FROM ENDRINGSLOGG WHERE aktivitet_id = ?",
                aktivitetId
        );

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


    public int endreAktivitetStatus(long aktivitetId, AktivitetStatus status) {
        return database.update("UPDATE AKTIVITET SET status = ? WHERE aktivitet_id = ?",
                getName(status),
                aktivitetId
        );
    }


}
