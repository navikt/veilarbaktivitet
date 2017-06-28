package no.nav.fo.veilarbaktivitet.db.dao;

import lombok.val;
import no.nav.fo.veilarbaktivitet.db.Database;
import no.nav.fo.veilarbaktivitet.db.rowmappers.AktivitetDataRowMapper;
import no.nav.fo.veilarbaktivitet.domain.*;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.Optional;

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
            "LEFT JOIN BEHANDLING B ON A.aktivitet_id = B.aktivitet_id AND A.versjon = B.versjon";

    private final Database database;

    @Inject
    public AktivitetDAO(Database database) {
        this.database = database;
    }

    public List<AktivitetData> hentAktiviteterForAktorId(String aktorId) {
        return database.query(SELECT_AKTIVITET +
                        "WHERE A.aktor_id = ? and A.gjeldende = 1",
                AktivitetDataRowMapper::mapAktivitet,
                aktorId
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


    public void insertAktivitet(AktivitetData aktivitet) {
        insertAktivitet(aktivitet, new Date());
    }

    @Transactional
    void insertAktivitet(AktivitetData aktivitet, Date endretDato) {

        database.update("UPDATE AKTIVITET SET gjeldende = 0 where aktivitet_id = ?", aktivitet.getId());

        val versjon = Optional.ofNullable(aktivitet.getVersjon()).map(v -> v + 1).orElse(0L);
        database.update("INSERT INTO AKTIVITET(aktivitet_id, versjon, aktor_id, type," +
                        "fra_dato, til_dato, tittel, beskrivelse, status," +
                        "avsluttet_kommentar, opprettet_dato, endret_dato, endret_av, lagt_inn_av, lenke, " +
                        "avtalt, gjeldende, transaksjons_type, historisk_dato) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
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
                endretDato,
                aktivitet.getEndretAv(),
                getName(aktivitet.getLagtInnAv()),
                aktivitet.getLenke(),
                aktivitet.isAvtalt(),
                true,
                getName(aktivitet.getTransaksjonsType()),
                aktivitet.getHistoriskDato()
        );

        insertStillingsSoek(aktivitet.getId(), versjon, aktivitet.getStillingsSoekAktivitetData());
        insertEgenAktivitet(aktivitet.getId(), versjon, aktivitet.getEgenAktivitetData());
        insertSokeAvtale(aktivitet.getId(), versjon, aktivitet.getSokeAvtaleAktivitetData());
        insertIJobb(aktivitet.getId(), versjon, aktivitet.getIJobbAktivitetData());
        insertBehandling(aktivitet.getId(), versjon, aktivitet.getBehandlingAktivitetData());

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
                .ifPresent(sokeAvtale -> database.update("INSERT INTO SOKEAVTALE(aktivitet_id, versjon, antall, avtale_oppfolging) " +
                                "VALUES(?,?,?,?)",
                        aktivitetId,
                        versjon,
                        sokeAvtale.getAntall(),
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
                            iJobb.getAnsttelsesforhold(),
                            iJobb.getArbeidstid()
                    );
                });
    }


    private void insertBehandling(long aktivitetId, long versjon, BehandlingAktivitetData behandlingAktivitet) {
        ofNullable(behandlingAktivitet)
                .ifPresent(behandling -> {
                    database.update("INSERT INTO BEHANDLING(aktivitet_id, versjon, behandling_sted," +
                                    " effekt, behandling_oppfolging",
                            aktivitetId,
                            versjon,
                            behandling.getBehandlingSted(),
                            behandling.getEffekt(),
                            behandling.getBehandlingOppfolging()
                    );
                });
    }

    public void slettAktivitet(long aktivitetId) {

        database.update("DELETE FROM EGENAKTIVITET WHERE aktivitet_id = ?",
                aktivitetId
        );
        database.update("DELETE FROM STILLINGSSOK WHERE aktivitet_id = ?",
                aktivitetId
        );
        database.update("DELETE FROM SOKEAVTALE WHERE aktivitet_id = ?",
                aktivitetId
        );
        database.update("DELETE FROM IJOBB WHERE aktivitet_id = ?",
                aktivitetId
        );
        database.update("DELETE FROM BEHANDLING WHERE aktivitet_id = ?",
                aktivitetId
        );

        database.update("DELETE FROM AKTIVITET WHERE aktivitet_id = ?",
                aktivitetId
        );
    }

    public List<AktivitetData> hentAktivitetVersjoner(long aktivitetId) {
        return database.query(SELECT_AKTIVITET +
                        "WHERE A.aktivitet_id = ? " +
                        "ORDER BY A.versjon desc",
                AktivitetDataRowMapper::mapAktivitet,
                aktivitetId
        );
    }
}
