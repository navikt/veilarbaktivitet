package no.nav.fo.veilarbaktivitet.db.dao;

import no.nav.fo.veilarbaktivitet.db.Database;
import no.nav.fo.veilarbaktivitet.db.rowmappers.AktivitetDataRowMapper;
import no.nav.fo.veilarbaktivitet.domain.*;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static no.nav.fo.veilarbaktivitet.util.EnumUtils.getName;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class AktivitetDAO {

    private static final Logger LOG = getLogger(AktivitetDAO.class);
    private static final String SELECT_AKTIVITET = "SELECT * FROM AKTIVITET A " +
            "LEFT JOIN STILLINGSSOK S ON A.aktivitet_id = S.aktivitet_id AND A.versjon = S.versjon " +
            "LEFT JOIN EGENAKTIVITET E ON A.aktivitet_id = E.aktivitet_id AND A.versjon = E.versjon " +
            "LEFT JOIN SOKEAVTALE SA ON A.aktivitet_id = SA.aktivitet_id AND A.versjon = SA.versjon " +
            "LEFT JOIN IJOBB IJ ON A.aktivitet_id = IJ.aktivitet_id AND A.versjon = IJ.versjon " +
            "LEFT JOIN MOTE M ON A.aktivitet_id = M.aktivitet_id AND A.versjon = M.versjon " +
            "LEFT JOIN BEHANDLING B ON A.aktivitet_id = B.aktivitet_id AND A.versjon = B.versjon ";

    private final Database database;

    @Inject
    public AktivitetDAO(Database database) {
        this.database = database;
    }

    public List<AktivitetData> hentAktiviteterForAktorId(Person.AktorId aktorId) {
        return database.query(SELECT_AKTIVITET +
                        "WHERE A.aktor_id = ? and A.gjeldende = 1",
                AktivitetDataRowMapper::mapAktivitet,
                aktorId.get()
        );
    }

    public AktivitetData hentAktivitet(long aktivitetId) {
        return database.queryForObject(SELECT_AKTIVITET +
                        "WHERE A.aktivitet_id = ? and gjeldende = 1",
                AktivitetDataRowMapper::mapAktivitet,
                aktivitetId
        );
    }

    public long getNextUniqueAktivitetId() {
        return database.nesteFraSekvens("AKTIVITET_ID_SEQ");
    }

    public long nesteVersjon() {
        return database.nesteFraSekvens("AKTIVITET_VERSJON_SEQ");
    }

    @Transactional
    public void insertAktivitet(AktivitetData aktivitet) {
        insertAktivitet(aktivitet, new Date());
    }

    @Transactional
    public void insertAktivitet(AktivitetData aktivitet, Date endretDato) {
        long aktivitetId = aktivitet.getId();
        database.update("UPDATE AKTIVITET SET gjeldende = 0 where aktivitet_id = ?", aktivitetId);

        long versjon = nesteVersjon();
        database.update("INSERT INTO AKTIVITET(aktivitet_id, versjon, aktor_id, aktivitet_type_kode," +
                        "fra_dato, til_dato, tittel, beskrivelse, livslopstatus_kode," +
                        "avsluttet_kommentar, opprettet_dato, endret_dato, endret_av, lagt_inn_av, lenke, " +
                        "avtalt, gjeldende, transaksjons_type, historisk_dato, kontorsperre_enhet_id, mal_id) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                aktivitetId,
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
                endretDato,
                aktivitet.getEndretAv(),
                getName(aktivitet.getLagtInnAv()),
                aktivitet.getLenke(),
                aktivitet.isAvtalt(),
                true,
                getName(aktivitet.getTransaksjonsType()),
                aktivitet.getHistoriskDato(),
                aktivitet.getKontorsperreEnhetId(),
                aktivitet.getMalid()
        );

        insertStillingsSoek(aktivitetId, versjon, aktivitet.getStillingsSoekAktivitetData());
        insertEgenAktivitet(aktivitetId, versjon, aktivitet.getEgenAktivitetData());
        insertSokeAvtale(aktivitetId, versjon, aktivitet.getSokeAvtaleAktivitetData());
        insertIJobb(aktivitetId, versjon, aktivitet.getIJobbAktivitetData());
        insertBehandling(aktivitetId, versjon, aktivitet.getBehandlingAktivitetData());
        insertMote(aktivitetId, versjon, aktivitet.getMoteData());

        LOG.info("opprettet {}", aktivitet);
    }

    private void insertMote(long aktivitetId, long versjon, MoteData moteData) {
        ofNullable(moteData).ifPresent(m -> {
            database.update("INSERT INTO MOTE(" +
                            "aktivitet_id, " +
                            "versjon, " +
                            "adresse," +
                            "forberedelser, " +
                            "kanal, " +
                            "referat, " +
                            "referat_publisert" +
                            ") VALUES(?,?,?,?,?,?,?)",
                    aktivitetId,
                    versjon,
                    moteData.getAdresse(),
                    moteData.getForberedelser(),
                    getName(moteData.getKanal()),
                    moteData.getReferat(),
                    moteData.isReferatPublisert()
            );
        });
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
                .ifPresent(egen -> database.update("INSERT INTO EGENAKTIVITET(aktivitet_id, versjon, hensikt, oppfolging) " +
                                "VALUES(?,?,?,?)",
                        aktivitetId,
                        versjon,
                        egen.getHensikt(),
                        egen.getOppfolging()
                ));
    }

    private void insertSokeAvtale(long aktivitetId, long versjon, SokeAvtaleAktivitetData sokeAvtaleAktivitetData) {
        ofNullable(sokeAvtaleAktivitetData)
                .ifPresent(sokeAvtale -> database.update("INSERT INTO SOKEAVTALE(aktivitet_id, versjon, antall_stillinger_sokes, avtale_oppfolging) " +
                                "VALUES(?,?,?,?)",
                        aktivitetId,
                        versjon,
                        sokeAvtale.getAntallStillingerSokes(),
                        sokeAvtale.getAvtaleOppfolging()
                ));
    }

    private void insertIJobb(long aktivitetId, long versjon, IJobbAktivitetData iJobbAktivitet) {
        ofNullable(iJobbAktivitet)
                .ifPresent(iJobb -> {
                    database.update("INSERT INTO IJOBB(aktivitet_id, versjon, jobb_status," +
                                    " ansettelsesforhold, arbeidstid) VALUES(?,?,?,?,?)",
                            aktivitetId,
                            versjon,
                            getName(iJobb.getJobbStatusType()),
                            iJobb.getAnsettelsesforhold(),
                            iJobb.getArbeidstid()
                    );
                });
    }


    private void insertBehandling(long aktivitetId, long versjon, BehandlingAktivitetData behandlingAktivitet) {
        ofNullable(behandlingAktivitet)
                .ifPresent(behandling -> {
                    database.update("INSERT INTO BEHANDLING(aktivitet_id, versjon, behandling_type, " +
                                    "behandling_sted, effekt, behandling_oppfolging) VALUES(?,?,?,?,?,?)",
                            aktivitetId,
                            versjon,
                            behandling.getBehandlingType(),
                            behandling.getBehandlingSted(),
                            behandling.getEffekt(),
                            behandling.getBehandlingOppfolging()
                    );
                });
    }

    @Transactional
    public void slettAktivitet(long aktivitetId) {
        int oppdaterteRader = Stream.of(
                "DELETE FROM EGENAKTIVITET WHERE aktivitet_id = ?",
                "DELETE FROM STILLINGSSOK WHERE aktivitet_id = ?",
                "DELETE FROM SOKEAVTALE WHERE aktivitet_id = ?",
                "DELETE FROM IJOBB WHERE aktivitet_id = ?",
                "DELETE FROM BEHANDLING WHERE aktivitet_id = ?",
                "DELETE FROM MOTE WHERE aktivitet_id = ?",
                "DELETE FROM AKTIVITET WHERE aktivitet_id = ?"
        )
                .mapToInt((sql) -> database.update(sql, aktivitetId))
                .sum();

        if (oppdaterteRader > 0) {
            database.update("INSERT INTO SLETTEDE_AKTIVITETER(aktivitet_id, tidspunkt) VALUES(?, CURRENT_TIMESTAMP)", aktivitetId);
        }
    }

    public List<AktivitetData> hentAktivitetVersjoner(long aktivitetId) {
        return database.query(SELECT_AKTIVITET +
                        "WHERE A.aktivitet_id = ? " +
                        "ORDER BY A.versjon desc",
                AktivitetDataRowMapper::mapAktivitet,
                aktivitetId
        );
    }

    @Transactional
    public boolean kasserAktivitet(long aktivitetId) {
        String whereClause = "WHERE aktivitet_id = ?";
        int oppdaterteRader = Stream.of(
                "UPDATE EGENAKTIVITET SET HENSIKT = 'Kassert av NAV', OPPFOLGING = 'Kassert av NAV'",
                "UPDATE STILLINGSSOK SET ARBEIDSGIVER = 'Kassert av NAV', STILLINGSTITTEL = 'Kassert av NAV', KONTAKTPERSON = 'Kassert av NAV', ETIKETT = null, ARBEIDSSTED = 'Kassert av NAV'",
                "UPDATE SOKEAVTALE SET ANTALL_STILLINGER_SOKES = 0, AVTALE_OPPFOLGING = 'Kassert av NAV'",
                "UPDATE IJOBB SET ANSETTELSESFORHOLD = 'Kassert av NAV', ARBEIDSTID = 'Kassert av NAV'",
                "UPDATE BEHANDLING SET BEHANDLING_STED = 'Kassert av NAV', EFFEKT = 'Kassert av NAV', BEHANDLING_OPPFOLGING = 'Kassert av NAV', BEHANDLING_TYPE = 'Kassert av NAV'",
                "UPDATE MOTE SET ADRESSE = 'Kassert av NAV', FORBEREDELSER = 'Kassert av NAV', REFERAT = 'Kassert av NAV'",
                "UPDATE AKTIVITET SET TITTEL = 'Kassert av NAV', AVSLUTTET_KOMMENTAR = 'Kassert av NAV', LENKE = 'Kassert av NAV', BESKRIVELSE = 'Kassert av NAV'"
        )
                .map((sql) -> sql + " " + whereClause)
                .mapToInt((sql) -> database.update(sql, aktivitetId))
                .sum();

        return oppdaterteRader > 0;
    }

    @Transactional
    public void insertLestAvBrukerTidspunkt(long aktivitetId) {
        database.update("UPDATE AKTIVITET SET LEST_AV_BRUKER_FORSTE_GANG = CURRENT_TIMESTAMP " +
                "WHERE aktivitet_id = ?", aktivitetId);
    }
}
