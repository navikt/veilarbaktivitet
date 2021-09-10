package no.nav.veilarbaktivitet.db.rowmappers;

import lombok.val;
import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.domain.*;
import no.nav.veilarbaktivitet.stilling_fra_nav.CvKanDelesData;
import no.nav.veilarbaktivitet.stilling_fra_nav.StillingFraNavData;
import no.nav.veilarbaktivitet.util.EnumUtils;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class AktivitetDataRowMapper implements RowMapper<AktivitetData> {

    @Override
    public AktivitetData mapRow(ResultSet rs, int rowNum) throws SQLException {
        return mapAktivitet(rs);
    }

    public static AktivitetData mapAktivitet(ResultSet rs) throws SQLException {
        val type = AktivitetTypeData.valueOf(rs.getString("aktivitet_type_kode"));
        val aktivitet = AktivitetData
                .builder()
                .id(rs.getLong("aktivitet_id"))
                .versjon(rs.getLong("versjon"))
                .aktorId(rs.getString("aktor_id"))
                .aktivitetType(type)
                .fraDato(Database.hentDato(rs, "fra_dato"))
                .tilDato(Database.hentDato(rs, "til_dato"))
                .tittel(rs.getString("tittel"))
                .beskrivelse(rs.getString("beskrivelse"))
                .status(EnumUtils.valueOf(AktivitetStatus.class, rs.getString("livslopstatus_kode")))
                .avsluttetKommentar(rs.getString("avsluttet_kommentar"))
                .opprettetDato(Database.hentDato(rs, "opprettet_dato"))
                .endretDato(Database.hentDato(rs, "endret_dato"))
                .endretAv(rs.getString("endret_av"))
                .lagtInnAv(EnumUtils.valueOf(InnsenderData.class, rs.getString("lagt_inn_av")))
                .avtalt(rs.getBoolean("avtalt"))
                .lenke(rs.getString("lenke"))
                .transaksjonsType(
                        EnumUtils.valueOf(AktivitetTransaksjonsType.class,
                                rs.getString("transaksjons_type"))
                )
                .historiskDato(Database.hentDato(rs, "historisk_dato"))
                .kontorsperreEnhetId(rs.getString("kontorsperre_enhet_id"))
                .lestAvBrukerForsteGang(Database.hentDato(rs, "lest_av_bruker_forste_gang"))
                .automatiskOpprettet(rs.getBoolean("automatisk_opprettet"))
                .malid(rs.getString("mal_id"))
                .fhoId(rs.getString("fho_id"));

        switch (type) {
            case EGENAKTIVITET:
                aktivitet.egenAktivitetData(mapEgenAktivitet(rs));
                break;
            case JOBBSOEKING:
                aktivitet.stillingsSoekAktivitetData(mapStillingsAktivitet(rs));
                break;
            case SOKEAVTALE:
                aktivitet.sokeAvtaleAktivitetData(mapSokeAvtaleAktivitet(rs));
                break;
            case IJOBB:
                aktivitet.iJobbAktivitetData(mapIJobbAktivitet(rs));
                break;
            case BEHANDLING:
                aktivitet.behandlingAktivitetData(mapBehandlingAktivitet(rs));
                break;
            case STILLING_FRA_NAV:
                aktivitet.stillingFraNavData(mapStillingFraNav(rs));
                break;
            case MOTE:
            case SAMTALEREFERAT:
                aktivitet.moteData(mapMoteData(rs));
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }

        return aktivitet.build();
    }

    private static MoteData mapMoteData(ResultSet rs) throws SQLException {
        return MoteData.builder()
                .adresse(rs.getString("adresse"))
                .forberedelser(rs.getString("forberedelser"))
                .kanal(Optional.ofNullable(rs.getString("kanal")).map(KanalDTO::valueOf).orElse(null))
                .referat(rs.getString("referat"))
                .referatPublisert(rs.getBoolean("referat_publisert"))
                .build()
                ;
    }

    private static StillingsoekAktivitetData mapStillingsAktivitet(ResultSet rs) throws SQLException {
        return StillingsoekAktivitetData.builder()
                .stillingsTittel(rs.getString("stillingstittel"))
                .arbeidsgiver(rs.getString("arbeidsgiver"))
                .arbeidssted(rs.getString("arbeidssted"))
                .kontaktPerson(rs.getString("kontaktperson"))
                .stillingsoekEtikett(EnumUtils.valueOf(StillingsoekEtikettData.class, rs.getString("etikett")))
                .build()
                ;
    }

    private static EgenAktivitetData mapEgenAktivitet(ResultSet rs) throws SQLException {
        return EgenAktivitetData.builder()
                .hensikt(rs.getString("hensikt"))
                .oppfolging(rs.getString("oppfolging"))
                .build();
    }

    private static SokeAvtaleAktivitetData mapSokeAvtaleAktivitet(ResultSet rs) throws SQLException {
        return SokeAvtaleAktivitetData.builder()
                .antallStillingerSokes(rs.getLong("antall_stillinger_sokes"))
                .antallStillingerIUken(rs.getLong("antall_stillinger_i_uken"))
                .avtaleOppfolging(rs.getString("avtale_oppfolging"))
                .build();
    }

    private static IJobbAktivitetData mapIJobbAktivitet(ResultSet rs) throws SQLException {
        return IJobbAktivitetData.builder()
                .jobbStatusType(EnumUtils.valueOf(JobbStatusTypeData.class, rs.getString("jobb_status")))
                .ansettelsesforhold(rs.getString("ansettelsesforhold"))
                .arbeidstid(rs.getString("arbeidstid"))
                .build();
    }

    private static BehandlingAktivitetData mapBehandlingAktivitet(ResultSet rs) throws SQLException {
        return BehandlingAktivitetData.builder()
                .behandlingType(rs.getString("behandling_type"))
                .behandlingSted(rs.getString("behandling_sted"))
                .effekt(rs.getString("effekt"))
                .behandlingOppfolging(rs.getString("behandling_oppfolging"))
                .build();
    }

    private static StillingFraNavData mapStillingFraNav(ResultSet rs) throws SQLException {
        //TODO fiks
        // soknadsfrist, svarFrist, arbeidsgiver, bestillingsId‚ stillingsId, arbeidsSted, varsel
        var cvKanDelesData = CvKanDelesData.builder()
                .kanDeles(rs.getObject("cv_kan_deles", Boolean.class))
                .endretAv(rs.getString("cv_kan_deles_av"))
                .endretTidspunkt(Database.hentDato(rs, "cv_kan_deles_tidspunkt"))
                .endretAvType(EnumUtils.valueOf(InnsenderData.class, rs.getString("cv_kan_deles_av_type")))
                .build();

        return StillingFraNavData.builder()
                .cvKanDelesData(cvKanDelesData.getKanDeles() == null ? null: cvKanDelesData)
                .soknadsfrist(rs.getString("soknadsfrist"))
                .svarfrist(Database.hentDato(rs, "svarFrist"))
                .arbeidsgiver(rs.getString("STILLING_FRA_NAV.ARBEIDSGIVER"))
                .bestillingsId(rs.getString("bestillingsId"))
                .stillingsId(rs.getString("stillingsId"))
                .arbeidssted(rs.getString("STILLING_FRA_NAV.ARBEIDSSTED"))
                .varselId(rs.getString("varselid"))
                .build();
    }

}
