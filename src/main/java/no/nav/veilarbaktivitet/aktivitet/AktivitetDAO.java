package no.nav.veilarbaktivitet.aktivitet;

import kotlin.Pair;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.aktivitet.domain.*;
import no.nav.veilarbaktivitet.config.database.Database;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.stilling_fra_nav.CvKanDelesData;
import no.nav.veilarbaktivitet.stilling_fra_nav.KontaktpersonData;
import no.nav.veilarbaktivitet.stilling_fra_nav.StillingFraNavData;
import no.nav.veilarbaktivitet.util.EnumUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.*;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

@Repository
@Slf4j
@RequiredArgsConstructor
public class AktivitetDAO {

    // Duplicate column names must _all_ be aliased

    // language=sql
    private static final String SELECT_AKTIVITET = """
            SELECT
            SFN.ARBEIDSGIVER as "STILLING_FRA_NAV.ARBEIDSGIVER", SFN.ARBEIDSSTED as "STILLING_FRA_NAV.ARBEIDSSTED",
            S.ARBEIDSGIVER AS "STILLINGSSOK.ARBEIDSGIVER", S.ARBEIDSSTED AS "STILLINGSSOK.ARBEIDSSTED",
            SFN.DETALJER as "STILLING_FRA_NAV.DETALJER",
            T.DETALJER AS "EKSTERNAKTIVITET.DETALJER",
            A.*, S.*, E.*, SA.*, IJ.*, M.*, B.*, SFN.*, T.* FROM AKTIVITET A
            LEFT JOIN STILLINGSSOK S ON A.aktivitet_id = S.aktivitet_id AND A.versjon = S.versjon
            LEFT JOIN EGENAKTIVITET E ON A.aktivitet_id = E.aktivitet_id AND A.versjon = E.versjon
            LEFT JOIN SOKEAVTALE SA ON A.aktivitet_id = SA.aktivitet_id AND A.versjon = SA.versjon
            LEFT JOIN IJOBB IJ ON A.aktivitet_id = IJ.aktivitet_id AND A.versjon = IJ.versjon
            LEFT JOIN MOTE M ON A.aktivitet_id = M.aktivitet_id AND A.versjon = M.versjon
            LEFT JOIN BEHANDLING B ON A.aktivitet_id = B.aktivitet_id AND A.versjon = B.versjon
            LEFT JOIN STILLING_FRA_NAV SFN on A.aktivitet_id = SFN.aktivitet_id and A.versjon = SFN.versjon
            LEFT JOIN EKSTERNAKTIVITET T on A.AKTIVITET_ID = T.AKTIVITET_ID and A.VERSJON = T.VERSJON
            """;

    private final Database database;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private static final String AKTIVITETID = "aktivitet_id";
    private static final String VERSJON = "versjon";

    private static final RowMapper<AktivitetData> aktivitetsDataRowMapper = (rs, rowNum) -> AktivitetDataRowMapper.mapAktivitet(rs);

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

    public Optional<AktivitetData> hentMaybeAktivitet(long id) {
        try {
            return Optional.of(hentAktivitet(id));
        } catch (DataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<AktivitetData> hentAktivitetByFunksjonellId(@NonNull UUID funksjonellId) {

        AktivitetData aktivitetData;
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("funksjonellId", funksjonellId.toString());
            aktivitetData = namedParameterJdbcTemplate.queryForObject(SELECT_AKTIVITET +
                                                        " WHERE A.funksjonell_id = :funksjonellId and gjeldende = 1",
                    params,
                    aktivitetsDataRowMapper
            );
        } catch (EmptyResultDataAccessException e) {
            return empty();
        }
        // language=sql
        return Optional.ofNullable(aktivitetData);
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


    public AktivitetData oppdaterAktivitet(AktivitetData aktivitet) {
        long aktivitetId = aktivitet.getId();

        SqlParameterSource selectGjeldendeParams = new MapSqlParameterSource(AKTIVITETID, aktivitetId);
        // Denne 'select for update' sørger for å låse gjeldende versjon for å hindre race-conditions
        // slik at ikke flere kan oppdatere samme aktivitet samtidig.
        //language=SQL
        long gjeldendeVersjon = database.getNamedJdbcTemplate().queryForObject("SELECT VERSJON FROM AKTIVITET where aktivitet_id = :aktivitet_id AND gjeldende=1 FOR UPDATE NOWAIT", selectGjeldendeParams, Long.class);
        if (aktivitet.getVersjon() != gjeldendeVersjon) {
            log.error("Forsøker å oppdatere en gammel aktivitet! aktitetsversjon: {} - gjeldende versjon: {}", aktivitet.getVersjon(), gjeldendeVersjon);
            throw new IllegalStateException("Forsøker å oppdatere en utdatert aktivitetsversjon.");
        }

        long versjon = nesteVersjon();

        AktivitetData nyAktivitetVersjon = insertAktivitetVersjon(aktivitet, aktivitetId, versjon);

        SqlParameterSource updateGjeldendeParams = new MapSqlParameterSource()
                .addValue(AKTIVITETID, aktivitetId)
                .addValue(VERSJON, gjeldendeVersjon);
        // language=sql
        database.getNamedJdbcTemplate().update("UPDATE AKTIVITET SET gjeldende = 0 where aktivitet_id = :aktivitet_id and versjon=:versjon", updateGjeldendeParams);

        return nyAktivitetVersjon;
    }

    private AktivitetData insertAktivitetVersjon(AktivitetData aktivitet, long aktivitetId, long versjon) {
        if (aktivitet.getOpprettetDato() == null || aktivitet.getEndretDato() == null) {
            throw new DataIntegrityViolationException("OpprettetDato og endretDato må være satt for aktivitetId: {}");
        }

        SqlParameterSource params = new MapSqlParameterSource()
                .addValue(AKTIVITETID, aktivitetId)
                .addValue(VERSJON, versjon)
                .addValue("aktor_id", aktivitet.getAktorId())
                .addValue("aktivitet_type_kode", aktivitet.getAktivitetType().name())
                .addValue("fra_dato", aktivitet.getFraDato())
                .addValue("til_dato", aktivitet.getTilDato())
                .addValue("tittel", aktivitet.getTittel())
                .addValue("beskrivelse", aktivitet.getBeskrivelse())
                .addValue("livslopstatus_kode", EnumUtils.getName(aktivitet.getStatus()))
                .addValue("avsluttet_kommentar", aktivitet.getAvsluttetKommentar())
                .addValue("opprettet_dato", aktivitet.getOpprettetDato())
                .addValue("endret_dato", aktivitet.getEndretDato())
                .addValue("endret_av", aktivitet.getEndretAv())
                .addValue("lagt_inn_av", EnumUtils.getName(aktivitet.getEndretAvType()))
                .addValue("lenke", aktivitet.getLenke())
                .addValue("avtalt", aktivitet.isAvtalt())
                .addValue("gjeldende", true)
                .addValue("transaksjons_type", EnumUtils.getName(aktivitet.getTransaksjonsType()))
                .addValue("historisk_dato", aktivitet.getHistoriskDato())
                .addValue("kontorsperre_enhet_id", aktivitet.getKontorsperreEnhetId())
                .addValue("automatisk_opprettet", aktivitet.isAutomatiskOpprettet())
                .addValue("mal_id", aktivitet.getMalid())
                .addValue("fho_id", aktivitet.getFhoId())
                .addValue("funksjonell_id", Optional.ofNullable(aktivitet.getFunksjonellId()).map(UUID::toString).orElse(null))
                .addValue("oppfolgingsperiode_uuid", aktivitet.getOppfolgingsperiodeId() != null
                        ? aktivitet.getOppfolgingsperiodeId().toString() : null);
        //language=SQL
        database.getNamedJdbcTemplate().update(
                """
                        INSERT INTO AKTIVITET(aktivitet_id, versjon, aktor_id, aktivitet_type_kode,
                        fra_dato, til_dato, tittel, beskrivelse, livslopstatus_kode,
                        avsluttet_kommentar, opprettet_dato, endret_dato, endret_av, lagt_inn_av, lenke,
                        avtalt, gjeldende, transaksjons_type, historisk_dato, kontorsperre_enhet_id, 
                        automatisk_opprettet, mal_id, fho_id, oppfolgingsperiode_uuid, FUNKSJONELL_ID)
                        VALUES (:aktivitet_id, :versjon, :aktor_id, :aktivitet_type_kode, :fra_dato,
                        :til_dato, :tittel, :beskrivelse, :livslopstatus_kode, :avsluttet_kommentar,
                        :opprettet_dato, :endret_dato, :endret_av, :lagt_inn_av, :lenke, :avtalt,
                        :gjeldende, :transaksjons_type, :historisk_dato, :kontorsperre_enhet_id,
                        :automatisk_opprettet, :mal_id, :fho_id, :oppfolgingsperiode_uuid, :funksjonell_id)
                        """, params);


        insertStillingsSoek(aktivitetId, versjon, aktivitet.getStillingsSoekAktivitetData());
        insertEgenAktivitet(aktivitetId, versjon, aktivitet.getEgenAktivitetData());
        insertSokeAvtale(aktivitetId, versjon, aktivitet.getSokeAvtaleAktivitetData());
        insertIJobb(aktivitetId, versjon, aktivitet.getIJobbAktivitetData());
        insertBehandling(aktivitetId, versjon, aktivitet.getBehandlingAktivitetData());
        insertMote(aktivitetId, versjon, aktivitet.getMoteData());
        insertStillingFraNav(aktivitetId, versjon, aktivitet.getStillingFraNavData());
        insertEksternAktivitet(aktivitetId, versjon, aktivitet.getEksternAktivitetData());

        AktivitetData nyAktivitet = aktivitet.withId(aktivitetId).withVersjon(versjon);

        log.info("opprettet {}", nyAktivitet);
        return nyAktivitet;
    }


    public AktivitetData opprettNyAktivitet(AktivitetData aktivitet) {
        long aktivitetId = nesteAktivitetId();
        long versjon = nesteVersjon();
        return insertAktivitetVersjon(aktivitet, aktivitetId, versjon);
    }

    private void insertMote(long aktivitetId, long versjon, MoteData moteData) {
        // language=sql
        String sql = """
                    INSERT INTO MOTE(aktivitet_id, versjon, adresse, forberedelser, kanal, referat, referat_publisert) VALUES (
                    :aktivitet_id,
                    :versjon,
                    :adresse,
                    :forberedelser,
                    :kanal,
                    :referat,
                    :referat_publisert)
                    """;
        ofNullable(moteData).ifPresent(m -> {
            SqlParameterSource params = new MapSqlParameterSource()
                    .addValue(AKTIVITETID, aktivitetId)
                    .addValue(VERSJON, versjon)
                    .addValue("adresse", moteData.getAdresse())
                    .addValue("forberedelser", moteData.getForberedelser())
                    .addValue("kanal", EnumUtils.getName(moteData.getKanal()))
                    .addValue("referat", moteData.getReferat())
                    .addValue("referat_publisert", moteData.isReferatPublisert());
            namedParameterJdbcTemplate.update(sql, params);
        });
    }

    private void insertStillingsSoek(long aktivitetId, long versjon, StillingsoekAktivitetData stillingsSoekAktivitet) {
        // language=sql
        String sql = """
                            INSERT INTO STILLINGSSOK(aktivitet_id, versjon, stillingstittel,
                            arbeidsgiver, arbeidssted, kontaktperson, etikett)
                            VALUES(:aktivitet_id, :versjon, :stillingstittel,
                            :arbeidsgiver, :arbeidssted, :kontaktperson, :etikett)
                            """;
        ofNullable(stillingsSoekAktivitet)
                .ifPresent(stillingsoek -> {
                    SqlParameterSource params = new MapSqlParameterSource()
                            .addValue(AKTIVITETID, aktivitetId)
                            .addValue(VERSJON, versjon)
                            .addValue("stillingstittel", stillingsoek.getStillingsTittel())
                            .addValue("arbeidsgiver", stillingsoek.getArbeidsgiver())
                            .addValue("arbeidssted", stillingsoek.getArbeidssted())
                            .addValue("kontaktperson", stillingsoek.getKontaktPerson())
                            .addValue("etikett", EnumUtils.getName(stillingsoek.getStillingsoekEtikett()));
                    namedParameterJdbcTemplate.update(sql, params);
                });
    }

    private void insertEgenAktivitet(long aktivitetId, long versjon, EgenAktivitetData egenAktivitetData) {
        ofNullable(egenAktivitetData)
                .ifPresent(egen -> {
                    SqlParameterSource params = new MapSqlParameterSource()
                            .addValue(AKTIVITETID, aktivitetId)
                            .addValue(VERSJON, versjon)
                            .addValue("hensikt", egen.getHensikt())
                            .addValue("oppfolging", egen.getOppfolging());
                    // language=sql
                    namedParameterJdbcTemplate.update("INSERT INTO EGENAKTIVITET(aktivitet_id, versjon, hensikt, oppfolging) " +
                                                           "VALUES(:aktivitet_id, :versjon, :hensikt, :oppfolging)",
                            params
                    );
                });
    }

    private void insertSokeAvtale(long aktivitetId, long versjon, SokeAvtaleAktivitetData sokeAvtaleAktivitetData) {
        ofNullable(sokeAvtaleAktivitetData)
                .ifPresent(sokeAvtale -> {
                    SqlParameterSource params = new MapSqlParameterSource()
                            .addValue(AKTIVITETID, aktivitetId)
                            .addValue(VERSJON, versjon)
                            .addValue("antall_stillinger_sokes", sokeAvtale.getAntallStillingerSokes())
                            .addValue("antall_stillinger_i_uken", sokeAvtale.getAntallStillingerIUken())
                            .addValue("avtale_oppfolging", sokeAvtale.getAvtaleOppfolging());
                    // language=sql
                    namedParameterJdbcTemplate.update(
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
                            .addValue(AKTIVITETID, aktivitetId)
                            .addValue(VERSJON, versjon)
                            .addValue("jobb_status", EnumUtils.getName(iJobb.getJobbStatusType()))
                            .addValue("ansettelsesforhold", iJobb.getAnsettelsesforhold())
                            .addValue("arbeidstid", iJobb.getArbeidstid());
                    // language=sql
                    namedParameterJdbcTemplate.update(
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
        // language=sql
        String sql = """
                            INSERT INTO BEHANDLING(aktivitet_id, versjon, behandling_type,
                            behandling_sted, effekt, behandling_oppfolging)
                            VALUES(:aktivitet_id, :versjon, :behandling_type,
                            :behandling_sted, :effekt, :behandling_oppfolging)
                            """;
        ofNullable(behandlingAktivitet)
                .ifPresent(behandling -> {
                    SqlParameterSource params = new MapSqlParameterSource()
                            .addValue(AKTIVITETID, aktivitetId)
                            .addValue(VERSJON, versjon)
                            .addValue("behandling_type", behandling.getBehandlingType())
                            .addValue("behandling_sted", behandling.getBehandlingSted())
                            .addValue("effekt", behandling.getEffekt())
                            .addValue("behandling_oppfolging", behandling.getBehandlingOppfolging());
                    namedParameterJdbcTemplate.update(
                            sql,
                            params);
                });
    }


    private void insertStillingFraNav(long aktivitetId, long versjon, StillingFraNavData stillingFraNavData) {

        // language=sql
        String sql = """ 
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
                            livslopsstatus,
                            detaljer
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
                            :livslopsstatus,
                            :detaljer)
                            """;
        ofNullable(stillingFraNavData)
                .ifPresent(stilling -> {
                            var cvKanDelesData = Optional.ofNullable(stilling.getCvKanDelesData());
                            var kontaktpersonData = Optional.ofNullable(stilling.getKontaktpersonData());
                            SqlParameterSource parms = new MapSqlParameterSource()
                                    .addValue(AKTIVITETID, aktivitetId)
                                    .addValue(VERSJON, versjon)
                                    .addValue("cv_kan_deles", cvKanDelesData.map(CvKanDelesData::getKanDeles).orElse(null))
                                    .addValue("cv_kan_deles_tidspunkt", cvKanDelesData.map(CvKanDelesData::getEndretTidspunkt).orElse(null))
                                    .addValue("cv_kan_deles_av", cvKanDelesData.map(CvKanDelesData::getEndretAv).orElse(null))
                                    .addValue("cv_kan_deles_av_type", cvKanDelesData.map(CvKanDelesData::getEndretAvType).map(Enum::name).orElse(null))
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
                                    .addValue("kontaktperson_mobil", kontaktpersonData.map(KontaktpersonData::getMobil).orElse(null))
                                    .addValue("soknadsstatus", EnumUtils.getName(stilling.getSoknadsstatus()))
                                    .addValue("livslopsstatus", EnumUtils.getName(stilling.getLivslopsStatus()))
                                    .addValue("detaljer", stilling.getDetaljer());
                    namedParameterJdbcTemplate.update(
                            sql,
                            parms
                            );
                        }
                );
    }

    private void insertEksternAktivitet(long aktivitetId, long versjon, EksternAktivitetData eksternAktivitetData) {
        Optional.ofNullable(eksternAktivitetData)
                .ifPresent(tiltak -> {
                    SqlParameterSource params = new MapSqlParameterSource()
                            .addValue(AKTIVITETID, aktivitetId)
                            .addValue(VERSJON, versjon)
                            .addValue("source", eksternAktivitetData.getSource())
                            .addValue("tiltak_kode", eksternAktivitetData.getTiltaksKode())
                            .addValue("arena_id", eksternAktivitetData.getArenaId() != null ? eksternAktivitetData.getArenaId().id() : null)
                            .addValue("oppfolgingsperiode_slutt", eksternAktivitetData.getOppfolgingsperiodeSlutt())
                            .addValue("opprettet_som_historisk", eksternAktivitetData.getOpprettetSomHistorisk())
                            .addValue("aktivitetkort_type", eksternAktivitetData.getType().name())
                            .addValue("oppgave", JsonUtils.toJson(eksternAktivitetData.getOppgave()))
                            .addValue("handlinger", JsonUtils.toJson(eksternAktivitetData.getHandlinger()))
                            .addValue("detaljer", JsonUtils.toJson(eksternAktivitetData.getDetaljer()))
                            .addValue("etiketter", JsonUtils.toJson(eksternAktivitetData.getEtiketter()));
                    // language=sql
                    namedParameterJdbcTemplate.update(
                    """
                        INSERT INTO EKSTERNAKTIVITET
                        (aktivitet_id, versjon, source, tiltak_kode, arena_id, oppfolgingsperiode_slutt, opprettet_som_historisk, aktivitetkort_type, oppgave, handlinger, detaljer, etiketter) VALUES
                        (:aktivitet_id, :versjon, :source, :tiltak_kode, :arena_id, :oppfolgingsperiode_slutt, :opprettet_som_historisk, :aktivitetkort_type, :oppgave, :handlinger, :detaljer, :etiketter)
                        """,
                    params
                    );
                });
    }

    public List<AktivitetData> hentAktivitetVersjoner(long aktivitetId) {
        // language=sql
        return database.query(
                SELECT_AKTIVITET +
                """
                            WHERE A.aktivitet_id = ?
                            ORDER BY A.versjon desc
                        """,
                AktivitetDataRowMapper::mapAktivitet,
                aktivitetId
        );
    }

    public Map<Long, Long> getAktivitetsVersjoner(List<Long> aktiviteter) {
        if (aktiviteter.isEmpty()) return Map.of();
        var params = new MapSqlParameterSource()
            .addValue("aktiviteter",  aktiviteter);
        // language=sql
        return database.getNamedJdbcTemplate().query("""
            SELECT AKTIVITET_ID, VERSJON FROM AKTIVITET where AKTIVITET.AKTIVITET_ID in (:aktiviteter)
        """, params, (rs, rowNum) -> new Pair<Long, Long>(rs.getLong("AKTIVITET_ID"), rs.getLong("VERSJON")))
            .stream()
            .reduce(new HashMap<>(), (mapping, pair) -> {
                mapping.put(pair.component1(), pair.component2());
                return mapping;
            }, (accumulatedMappings, nextSingleMapping) -> {
                accumulatedMappings.putAll(nextSingleMapping);
                return accumulatedMappings;
            });
    }



    public void insertLestAvBrukerTidspunkt(long aktivitetId) {
        var params = new MapSqlParameterSource().addValue("aktivitetId", aktivitetId);
        // language=sql
        namedParameterJdbcTemplate.update("""
                UPDATE AKTIVITET SET LEST_AV_BRUKER_FORSTE_GANG = CURRENT_TIMESTAMP
                WHERE aktivitet_id = :aktivitetId
                """, params);
    }
}
