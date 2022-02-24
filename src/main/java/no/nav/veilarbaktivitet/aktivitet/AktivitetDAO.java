package no.nav.veilarbaktivitet.aktivitet;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.aktivitet.domain.*;
import no.nav.veilarbaktivitet.avtalt_med_nav.Forhaandsorientering;
import no.nav.veilarbaktivitet.config.database.Database;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.stilling_fra_nav.CvKanDelesData;
import no.nav.veilarbaktivitet.stilling_fra_nav.KontaktpersonData;
import no.nav.veilarbaktivitet.stilling_fra_nav.StillingFraNavData;
import no.nav.veilarbaktivitet.util.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

@Repository
@Slf4j
public class AktivitetDAO {

    // TODO: Refaktorer spørring. Her joines mange tabeller som kan ha kolonner med samme navn. Hva som hentes ut av ResultSet kommer an på rekkefølgen det joines.
    // language=sql
    private static final String SELECT_AKTIVITET = """
            SELECT SFN.ARBEIDSGIVER as "STILLING_FRA_NAV.ARBEIDSGIVER", SFN.ARBEIDSSTED as "STILLING_FRA_NAV.ARBEIDSSTED", A.*, S.*, E.*, SA.*, IJ.*, M.*, B.*, SFN.* FROM AKTIVITET A
            LEFT JOIN STILLINGSSOK S ON A.aktivitet_id = S.aktivitet_id AND A.versjon = S.versjon
            LEFT JOIN EGENAKTIVITET E ON A.aktivitet_id = E.aktivitet_id AND A.versjon = E.versjon
            LEFT JOIN SOKEAVTALE SA ON A.aktivitet_id = SA.aktivitet_id AND A.versjon = SA.versjon
            LEFT JOIN IJOBB IJ ON A.aktivitet_id = IJ.aktivitet_id AND A.versjon = IJ.versjon
            LEFT JOIN MOTE M ON A.aktivitet_id = M.aktivitet_id AND A.versjon = M.versjon
            LEFT JOIN BEHANDLING B ON A.aktivitet_id = B.aktivitet_id AND A.versjon = B.versjon
            LEFT JOIN STILLING_FRA_NAV SFN on A.aktivitet_id = SFN.aktivitet_id and A.versjon = SFN.versjon\s""";

    private final Database database;

    @Autowired
    public AktivitetDAO(Database database) {
        this.database = database;
    }

    public List<AktivitetData> hentAktiviteterForOppfolgingsperiodeId(UUID oppfolgingsperiodeId) {
        // language=sql
        return database.query(SELECT_AKTIVITET +
                        " WHERE A.OPPFOLGINGSPERIODE_UUID = ? and A.GJELDENDE = 1",
                AktivitetDataRowMapper::mapAktivitet,
                oppfolgingsperiodeId.toString()
        );
    }

    public List<AktivitetData> hentAktiviteterForAktorId(Person.AktorId aktorId) {
        // language=sql
        return database.query(SELECT_AKTIVITET +
                        " WHERE A.AKTOR_ID = ? and A.gjeldende = 1",
                AktivitetDataRowMapper::mapAktivitet,
                aktorId.get()
        );
    }

    public AktivitetData hentAktivitet(long aktivitetId) {
        // language=sql
        return database.queryForObject(SELECT_AKTIVITET +
                        " WHERE A.aktivitet_id = ? and gjeldende = 1",
                AktivitetDataRowMapper::mapAktivitet,
                aktivitetId
        );
    }

    public AktivitetData hentAktivitetVersion(long version) {
        // language=sql
        return database.queryForObject(SELECT_AKTIVITET +
                        " WHERE A.VERSJON = ? ",
                AktivitetDataRowMapper::mapAktivitet,
                version
        );
    }

    public long nesteAktivitetId() {
        return database.nesteFraSekvens("AKTIVITET_ID_SEQ");
    }

    public long nesteVersjon() {
        return database.nesteFraSekvens("AKTIVITET_VERSJON_SEQ");
    }

    @Transactional
    public AktivitetData oppdaterAktivitet(AktivitetData aktivitet) {
        return oppdaterAktivitet(aktivitet, new Date());
    }

    @Transactional
    public AktivitetData oppdaterAktivitet(AktivitetData aktivitet, Date endretDato) {
        long aktivitetId = aktivitet.getId();

        SqlParameterSource selectGjeldendeParams = new MapSqlParameterSource("aktivitet_id", aktivitetId);
        // Denne 'select for update' sørger for å låse gjeldende versjon for å hindre race-conditions
        // slik at ikke flere kan oppdatere samme aktivitet samtidig.
        //language=SQL
        long gjeldendeVersjon = database.getNamedJdbcTemplate().queryForObject("SELECT VERSJON FROM AKTIVITET where aktivitet_id = :aktivitet_id AND gjeldende=1 FOR UPDATE NOWAIT", selectGjeldendeParams, Long.class);
        if (aktivitet.getVersjon() != gjeldendeVersjon) {
            log.error("Forsøker å oppdatere en gammel aktivitet! aktitetsversjon: {} - gjeldende versjon: {}", aktivitet.getVersjon(), gjeldendeVersjon);
            throw new IllegalStateException("Forsøker å oppdatere en utdatert aktivitetsversjon.");
        }

        long versjon = nesteVersjon();

        AktivitetData nyAktivitetVersjon = insertAktivitetVersjon(aktivitet, aktivitetId, versjon, endretDato);

        SqlParameterSource updateGjeldendeParams = new MapSqlParameterSource()
                .addValue("aktivitet_id", aktivitetId)
                .addValue("versjon", gjeldendeVersjon);
        // language=sql
        database.getNamedJdbcTemplate().update("UPDATE AKTIVITET SET gjeldende = 0 where aktivitet_id = :aktivitet_id and versjon=:versjon", updateGjeldendeParams);

        return nyAktivitetVersjon;
    }

    private AktivitetData insertAktivitetVersjon(AktivitetData aktivitet, long aktivitetId, long versjon, Date endretDato) {
        final String fhoId = ofNullable(aktivitet.getForhaandsorientering())
                .map(Forhaandsorientering::getId)
                .orElse(null);
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("aktivitet_id", aktivitetId)
                .addValue("versjon", versjon)
                .addValue("aktor_id", aktivitet.getAktorId())
                .addValue("aktivitet_type_kode", aktivitet.getAktivitetType().name())
                .addValue("fra_dato", aktivitet.getFraDato())
                .addValue("til_dato", aktivitet.getTilDato())
                .addValue("tittel", aktivitet.getTittel())
                .addValue("beskrivelse", aktivitet.getBeskrivelse())
                .addValue("livslopstatus_kode", EnumUtils.getName(aktivitet.getStatus()))
                .addValue("avsluttet_kommentar", aktivitet.getAvsluttetKommentar())
                .addValue("opprettet_dato", aktivitet.getOpprettetDato())
                .addValue("endret_dato", endretDato)
                .addValue("endret_av", aktivitet.getEndretAv())
                .addValue("lagt_inn_av", EnumUtils.getName(aktivitet.getLagtInnAv()))
                .addValue("lenke", aktivitet.getLenke())
                .addValue("avtalt", aktivitet.isAvtalt())
                .addValue("gjeldende", true)
                .addValue("transaksjons_type", EnumUtils.getName(aktivitet.getTransaksjonsType()))
                .addValue("historisk_dato", aktivitet.getHistoriskDato())
                .addValue("kontorsperre_enhet_id", aktivitet.getKontorsperreEnhetId())
                .addValue("automatisk_opprettet", aktivitet.isAutomatiskOpprettet())
                .addValue("mal_id", aktivitet.getMalid())
                .addValue("fho_id", fhoId)
                .addValue("oppfolgingsperiode_uuid", aktivitet.getOppfolgingsperiodeId()!= null ? aktivitet.getOppfolgingsperiodeId().toString() : null);
        //language=SQL
        database.getNamedJdbcTemplate().update(
                """
                        INSERT INTO AKTIVITET(aktivitet_id, versjon, aktor_id, aktivitet_type_kode,
                        fra_dato, til_dato, tittel, beskrivelse, livslopstatus_kode,
                        avsluttet_kommentar, opprettet_dato, endret_dato, endret_av, lagt_inn_av, lenke,
                        avtalt, gjeldende, transaksjons_type, historisk_dato, kontorsperre_enhet_id, automatisk_opprettet, mal_id, fho_id, oppfolgingsperiode_uuid)
                        VALUES (:aktivitet_id, :versjon, :aktor_id, :aktivitet_type_kode, :fra_dato,
                        :til_dato, :tittel, :beskrivelse, :livslopstatus_kode, :avsluttet_kommentar,
                        :opprettet_dato, :endret_dato, :endret_av, :lagt_inn_av, :lenke, :avtalt,
                        :gjeldende, :transaksjons_type, :historisk_dato, :kontorsperre_enhet_id,
                        :automatisk_opprettet, :mal_id, :fho_id, :oppfolgingsperiode_uuid
                        )
                        """, params);


        insertStillingsSoek(aktivitetId, versjon, aktivitet.getStillingsSoekAktivitetData());
        insertEgenAktivitet(aktivitetId, versjon, aktivitet.getEgenAktivitetData());
        insertSokeAvtale(aktivitetId, versjon, aktivitet.getSokeAvtaleAktivitetData());
        insertIJobb(aktivitetId, versjon, aktivitet.getIJobbAktivitetData());
        insertBehandling(aktivitetId, versjon, aktivitet.getBehandlingAktivitetData());
        insertMote(aktivitetId, versjon, aktivitet.getMoteData());
        insertStillingFraNav(aktivitetId, versjon, aktivitet.getStillingFraNavData());

        AktivitetData nyAktivitet = aktivitet.withId(aktivitetId).withVersjon(versjon).withEndretDato(endretDato);

        log.info("opprettet {}", nyAktivitet);
        return nyAktivitet;
    }

    public AktivitetData opprettNyAktivitet(AktivitetData aktivitet) {
        long aktivitetId = nesteAktivitetId();
        long versjon = nesteVersjon();
        Date endretDato = new Date();
        return insertAktivitetVersjon(aktivitet, aktivitetId, versjon, endretDato);
    }

    private void insertMote(long aktivitetId, long versjon, MoteData moteData) {
        ofNullable(moteData).ifPresent(m -> {
            SqlParameterSource params = new MapSqlParameterSource()
                    .addValue("aktivitet_id", aktivitetId)
                    .addValue("versjon", versjon)
                    .addValue("adresse", moteData.getAdresse())
                    .addValue("forberedelser", moteData.getForberedelser())
                    .addValue("kanal", EnumUtils.getName(moteData.getKanal()))
                    .addValue("referat", moteData.getReferat())
                    .addValue("referat_publisert", moteData.isReferatPublisert());
            // language=sql
            database.getNamedJdbcTemplate().update("""
                    INSERT INTO MOTE(aktivitet_id, versjon, adresse, forberedelser, kanal, referat, referat_publisert) VALUES (
                    :aktivitet_id,
                    :versjon,
                    :adresse,
                    :forberedelser,
                    :kanal,
                    :referat,
                    :referat_publisert)
                    """, params);
        });
    }

    private void insertStillingsSoek(long aktivitetId, long versjon, StillingsoekAktivitetData stillingsSoekAktivitet) {
        ofNullable(stillingsSoekAktivitet)
                .ifPresent(stillingsoek -> {
                    SqlParameterSource params = new MapSqlParameterSource()
                            .addValue("aktivitet_id", aktivitetId)
                            .addValue("versjon", versjon)
                            .addValue("stillingstittel", stillingsoek.getStillingsTittel())
                            .addValue("arbeidsgiver", stillingsoek.getArbeidsgiver())
                            .addValue("arbeidssted", stillingsoek.getArbeidssted())
                            .addValue("kontaktperson", stillingsoek.getKontaktPerson())
                            .addValue("etikett", EnumUtils.getName(stillingsoek.getStillingsoekEtikett()));
                    // language=sql
                    database.getNamedJdbcTemplate().update("""
                            INSERT INTO STILLINGSSOK(aktivitet_id, versjon, stillingstittel,
                            arbeidsgiver, arbeidssted, kontaktperson, etikett)
                            VALUES(:aktivitet_id, :versjon, :stillingstittel,
                            :arbeidsgiver, :arbeidssted, :kontaktperson, :etikett)
                            """, params);
                });
    }

    private void insertEgenAktivitet(long aktivitetId, long versjon, EgenAktivitetData egenAktivitetData) {
        ofNullable(egenAktivitetData)
                .ifPresent(egen -> {
                    SqlParameterSource params = new MapSqlParameterSource()
                            .addValue("aktivitet_id", aktivitetId)
                            .addValue("versjon", versjon)
                            .addValue("hensikt", egen.getHensikt())
                            .addValue("oppfolging", egen.getOppfolging());
                    // language=sql
                    database.getNamedJdbcTemplate().update("INSERT INTO EGENAKTIVITET(aktivitet_id, versjon, hensikt, oppfolging) " +
                                    "VALUES(:aktivitet_id, :versjon, :hensikt, :oppfolging)",
                            params
                    );
                });
    }

    private void insertSokeAvtale(long aktivitetId, long versjon, SokeAvtaleAktivitetData sokeAvtaleAktivitetData) {
        ofNullable(sokeAvtaleAktivitetData)
                .ifPresent(sokeAvtale -> {
                    SqlParameterSource params = new MapSqlParameterSource()
                            .addValue("aktivitet_id", aktivitetId)
                            .addValue("versjon", versjon)
                            .addValue("antall_stillinger_sokes", sokeAvtale.getAntallStillingerSokes())
                            .addValue("antall_stillinger_i_uken", sokeAvtale.getAntallStillingerIUken())
                            .addValue("avtale_oppfolging", sokeAvtale.getAvtaleOppfolging());
                    // language=sql
                    database.getNamedJdbcTemplate().update(
                            """
                                    INSERT INTO SOKEAVTALE(aktivitet_id, versjon, antall_stillinger_sokes, antall_stillinger_i_uken, avtale_oppfolging)
                                    VALUES(:aktivitet_id, :versjon, :antall_stillinger_sokes, :antall_stillinger_i_uken, :avtale_oppfolging)
                                    """,
                            params
                    );
                });
    }

    private void insertIJobb(long aktivitetId, long versjon, IJobbAktivitetData iJobbAktivitet) {
        ofNullable(iJobbAktivitet)
                .ifPresent(iJobb -> {
                    SqlParameterSource params = new MapSqlParameterSource()
                            .addValue("aktivitet_id", aktivitetId)
                            .addValue("versjon", versjon)
                            .addValue("jobb_status", EnumUtils.getName(iJobb.getJobbStatusType()))
                            .addValue("ansettelsesforhold", iJobb.getAnsettelsesforhold())
                            .addValue("arbeidstid", iJobb.getArbeidstid());
                    // language=sql
                    database.getNamedJdbcTemplate().update(
                            """
                                    INSERT INTO IJOBB(aktivitet_id, versjon, jobb_status,
                                    ansettelsesforhold, arbeidstid) VALUES(:aktivitet_id, :versjon, :jobb_status,
                                    :ansettelsesforhold, :arbeidstid)
                                    """,
                            params
                    );
                });
    }

    private void insertBehandling(long aktivitetId, long versjon, BehandlingAktivitetData behandlingAktivitet) {
        ofNullable(behandlingAktivitet)
                .ifPresent(behandling -> {
                    SqlParameterSource params = new MapSqlParameterSource()
                            .addValue("aktivitet_id", aktivitetId)
                            .addValue("versjon", versjon)
                            .addValue("behandling_type", behandling.getBehandlingType())
                            .addValue("behandling_sted", behandling.getBehandlingSted())
                            .addValue("effekt", behandling.getEffekt())
                            .addValue("behandling_oppfolging", behandling.getBehandlingOppfolging());
                    // language=sql
                    database.getNamedJdbcTemplate().update(
                            """
                                    INSERT INTO BEHANDLING(aktivitet_id, versjon, behandling_type,
                                    behandling_sted, effekt, behandling_oppfolging)
                                    VALUES(:aktivitet_id, :versjon, :behandling_type,
                                    :behandling_sted, :effekt, :behandling_oppfolging)
                                    """,
                            params);
                });
    }


    private void insertStillingFraNav(long aktivitetId, long versjon, StillingFraNavData stillingFraNavData) {
        ofNullable(stillingFraNavData)
                .ifPresent(stilling -> {
                            var cvKanDelesData = Optional.ofNullable(stilling.getCvKanDelesData());
                            var kontaktpersonData = Optional.ofNullable(stilling.getKontaktpersonData());
                            SqlParameterSource parms = new MapSqlParameterSource()
                                    .addValue("aktivitet_id", aktivitetId)
                                    .addValue("versjon", versjon)
                                    .addValue("cv_kan_deles", cvKanDelesData.map(CvKanDelesData::getKanDeles).orElse(null))
                                    .addValue("cv_kan_deles_tidspunkt", cvKanDelesData.map(CvKanDelesData::getEndretTidspunkt).orElse(null))
                                    .addValue("cv_kan_deles_av", cvKanDelesData.map(CvKanDelesData::getEndretAv).orElse(null))
                                    .addValue("cv_kan_deles_av_type", cvKanDelesData.map(CvKanDelesData::getEndretAvType).orElse(null))
                                    .addValue("cv_kan_deles_avtalt_dato", cvKanDelesData.map(CvKanDelesData::getAvtaltDato).orElse(null))
                                    .addValue("soknadsfrist", stilling.getSoknadsfrist())
                                    .addValue("svarfrist", stilling.getSvarfrist())
                                    .addValue("arbeidsgiver", stilling.getArbeidsgiver())
                                    .addValue("bestillingsId", stilling.getBestillingsId())
                                    .addValue("stillingsId", stilling.getStillingsId())
                                    .addValue("arbeidssted", stilling.getArbeidssted())
                                    .addValue("varselid", stilling.getVarselId())
                                    .addValue("kontaktperson_navn", kontaktpersonData.map(KontaktpersonData::getNavn).orElse(null))
                                    .addValue("kontaktperson_tittel", kontaktpersonData.map(KontaktpersonData::getTittel).orElse(null))
                                    .addValue("kontaktperson_mobil",  kontaktpersonData.map(KontaktpersonData::getMobil).orElse(null))
                                    .addValue("soknadsstatus", EnumUtils.getName(stilling.getSoknadsstatus()))
                                    .addValue("livslopsstatus", EnumUtils.getName(stilling.getLivslopsStatus()));

                            // language=sql
                            database.getNamedJdbcTemplate().update(
                                    """ 
                                            insert into STILLING_FRA_NAV (
                                            AKTIVITET_ID,
                                            VERSJON,
                                            CV_KAN_DELES,
                                            CV_KAN_DELES_TIDSPUNKT,
                                            CV_KAN_DELES_AV,
                                            CV_KAN_DELES_AV_TYPE,
                                            CV_KAN_DELES_AVTALT_DATO,
                                            soknadsfrist,
                                            svarfrist,
                                            arbeidsgiver,
                                            bestillingsId,
                                            stillingsId,
                                            arbeidssted,
                                            varselid,
                                            kontaktperson_navn,
                                            kontaktperson_tittel,
                                            kontaktperson_mobil,
                                            soknadsstatus,
                                            livslopsstatus
                                            ) VALUES (
                                            :aktivitet_id,
                                            :versjon,
                                            :cv_kan_deles,
                                            :cv_kan_deles_tidspunkt,
                                            :cv_kan_deles_av,
                                            :cv_kan_deles_av_type,
                                            :cv_kan_deles_avtalt_dato,
                                            :soknadsfrist ,
                                            :svarfrist ,
                                            :arbeidsgiver ,
                                            :bestillingsId ,
                                            :stillingsId ,
                                            :arbeidssted ,
                                            :varselid ,
                                            :kontaktperson_navn ,
                                            :kontaktperson_tittel ,
                                            :kontaktperson_mobil ,
                                            :soknadsstatus,
                                            :livslopsstatus)
                                            """,
                                    parms
                            );
                        }
                );
    }

    public List<AktivitetData> hentAktivitetVersjoner(long aktivitetId) {
        // language=sql
        return database.query(SELECT_AKTIVITET +
                        "WHERE A.aktivitet_id = ? " +
                        "ORDER BY A.versjon desc",
                AktivitetDataRowMapper::mapAktivitet,
                aktivitetId
        );
    }

    @Transactional
    public boolean kasserAktivitet(long aktivitetId) {
        // language=sql
        String whereClause = "WHERE aktivitet_id = ?";
        // language=sql
        int oppdaterteRader = Stream.of(
                        "UPDATE EGENAKTIVITET SET HENSIKT = 'Kassert av NAV', OPPFOLGING = 'Kassert av NAV'",
                        "UPDATE STILLINGSSOK SET ARBEIDSGIVER = 'Kassert av NAV', STILLINGSTITTEL = 'Kassert av NAV', KONTAKTPERSON = 'Kassert av NAV', ETIKETT = null, ARBEIDSSTED = 'Kassert av NAV'",
                        "UPDATE SOKEAVTALE SET ANTALL_STILLINGER_SOKES = 0, ANTALL_STILLINGER_I_UKEN = 0, AVTALE_OPPFOLGING = 'Kassert av NAV'",
                        "UPDATE IJOBB SET ANSETTELSESFORHOLD = 'Kassert av NAV', ARBEIDSTID = 'Kassert av NAV'",
                        "UPDATE BEHANDLING SET BEHANDLING_STED = 'Kassert av NAV', EFFEKT = 'Kassert av NAV', BEHANDLING_OPPFOLGING = 'Kassert av NAV', BEHANDLING_TYPE = 'Kassert av NAV'",
                        "UPDATE MOTE SET ADRESSE = 'Kassert av NAV', FORBEREDELSER = 'Kassert av NAV', REFERAT = 'Kassert av NAV'",
                        "UPDATE STILLING_FRA_NAV SET KONTAKTPERSON_NAVN = 'Kassert av NAV', KONTAKTPERSON_TITTEL = 'Kassert av NAV', KONTAKTPERSON_MOBIL = 'Kassert av NAV', ARBEIDSGIVER = 'Kassert av NAV', ARBEIDSSTED = 'Kassert av NAV', STILLINGSID = 'kassertAvNav', SOKNADSSTATUS = null",
                        "UPDATE AKTIVITET SET TITTEL = 'Det var skrevet noe feil, og det er nå slettet', AVSLUTTET_KOMMENTAR = 'Kassert av NAV', LENKE = 'Kassert av NAV', BESKRIVELSE = 'Kassert av NAV'"
                )
                .map(sql -> sql + " " + whereClause)
                .mapToInt(sql -> database.update(sql, aktivitetId))
                .sum();

        return oppdaterteRader > 0;
    }

    public void insertLestAvBrukerTidspunkt(long aktivitetId) {
        // language=sql
        database.update("""
                UPDATE AKTIVITET SET LEST_AV_BRUKER_FORSTE_GANG = CURRENT_TIMESTAMP
                WHERE aktivitet_id = ?
                """, aktivitetId);
    }
}
