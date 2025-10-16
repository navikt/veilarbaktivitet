package no.nav.veilarbaktivitet.aktivitet;

import no.nav.veilarbaktivitet.aktivitet.domain.*;
import no.nav.veilarbaktivitet.aktivitet.dto.KanalDTO;
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType;
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Attributt;
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Etikett;
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.LenkeSeksjon;
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Oppgaver;
import no.nav.veilarbaktivitet.arena.model.ArenaId;
import no.nav.veilarbaktivitet.config.database.Database;
import no.nav.veilarbaktivitet.person.Innsender;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.stilling_fra_nav.*;
import no.nav.veilarbaktivitet.util.EnumUtils;
import no.nav.veilarbaktivitet.veilarbdbutil.VeilarbAktivitetResultSet;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class AktivitetDataRowMapper implements RowMapper<AktivitetData> {

    @Override
    public AktivitetData mapRow(ResultSet rs, int rowNum) throws SQLException {
        return mapAktivitet(rs);
    }

    public static AktivitetData mapAktivitet(ResultSet resultSet) throws SQLException {
        final var rs = new VeilarbAktivitetResultSet(resultSet);
        final var type = AktivitetTypeData.valueOf(rs.getString("aktivitet_type_kode"));

        final var aktivitet = AktivitetData
                .builder()
                .id(rs.getLong("aktivitet_id"))
                .funksjonellId(Database.hentMaybeUUID(rs, "funksjonell_id"))
                .versjon(rs.getLong("versjon"))
                .aktorId(Person.aktorId(rs.getString("aktor_id")))
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
                .endretAvType(EnumUtils.valueOf(Innsender.class, rs.getString("lagt_inn_av")))
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
                .fhoId(rs.getString("fho_id"))
                .oppfolgingsperiodeId(Database.hentMaybeUUID(rs, "oppfolgingsperiode_uuid"));

        switch (type) {
            case EGENAKTIVITET -> aktivitet.egenAktivitetData(mapEgenAktivitet(rs));
            case JOBBSOEKING -> aktivitet.stillingsSoekAktivitetData(mapStillingsAktivitet(rs));
            case SOKEAVTALE -> aktivitet.sokeAvtaleAktivitetData(mapSokeAvtaleAktivitet(rs));
            case IJOBB -> aktivitet.iJobbAktivitetData(mapIJobbAktivitet(rs));
            case BEHANDLING -> aktivitet.behandlingAktivitetData(mapBehandlingAktivitet(rs));
            case STILLING_FRA_NAV -> aktivitet.stillingFraNavData(mapStillingFraNav(rs));
            case MOTE, SAMTALEREFERAT -> aktivitet.moteData(mapMoteData(rs));
            case EKSTERNAKTIVITET -> aktivitet.eksternAktivitetData(mapEksternAktivitetData((rs)));
            default -> throw new IllegalStateException("Unexpected value: " + type);
        }

        return aktivitet.build();
    }

    private static MoteData mapMoteData(ResultSet resultSet) throws SQLException {
        final var rs = new VeilarbAktivitetResultSet(resultSet);

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
        var etikett = rs.getString("etikett");
        return StillingsoekAktivitetData.builder()
                .stillingsTittel(rs.getString("stillingstittel"))
                .arbeidsgiver(rs.getString("STILLINGSSOK.ARBEIDSGIVER"))
                .arbeidssted(rs.getString("STILLINGSSOK.ARBEIDSSTED"))
                .kontaktPerson(rs.getString("kontaktperson"))
                .stillingsoekEtikett(etikett == null ? null : EnumUtils.valueOf(StillingsoekEtikettData.class, etikett))
                .build();
    }

    private static EgenAktivitetData mapEgenAktivitet(ResultSet rs) throws SQLException {
        return EgenAktivitetData.builder()
                .hensikt(rs.getString("hensikt"))
                .oppfolging(rs.getString("oppfolging"))
                .build();
    }

    private static SokeAvtaleAktivitetData mapSokeAvtaleAktivitet(ResultSet rs) throws SQLException {
        return SokeAvtaleAktivitetData.builder()
                .antallStillingerSokes(rs.getInt("antall_stillinger_sokes"))
                .antallStillingerIUken(rs.getInt("antall_stillinger_i_uken"))
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

    private static StillingFraNavData mapStillingFraNav(ResultSet resultSet) throws SQLException {
        VeilarbAktivitetResultSet rs = new VeilarbAktivitetResultSet(resultSet);

        var cvKanDelesData = CvKanDelesData.builder()
                .kanDeles(rs.getBooleanOrNull("cv_kan_deles"))
                .endretAv(rs.getString("cv_kan_deles_av"))
                .endretTidspunkt(Database.hentDato(rs, "cv_kan_deles_tidspunkt"))
                .avtaltDato(Database.hentDatoDato(rs, "cv_kan_deles_avtalt_dato"))
                .endretAvType(EnumUtils.valueOf(Innsender.class, rs.getString("cv_kan_deles_av_type")))
                .build();

        var kontaktpersonData = KontaktpersonData.builder()
                .navn(rs.getString("kontaktperson_navn"))
                .tittel(rs.getString("kontaktperson_tittel"))
                .mobil(rs.getString("kontaktperson_mobil"))
                .build();

        var soknadStatus = rs.getString("soknadsstatus");
        return StillingFraNavData.builder()
                .cvKanDelesData(cvKanDelesData.getKanDeles() == null ? null : cvKanDelesData)
                .soknadsfrist(rs.getString("soknadsfrist"))
                .svarfrist(Database.hentDato(rs, "svarFrist"))
                .arbeidsgiver(rs.getString("STILLING_FRA_NAV.ARBEIDSGIVER"))
                .bestillingsId(rs.getString("bestillingsId"))
                .stillingsId(rs.getString("stillingsId"))
                .arbeidssted(rs.getString("STILLING_FRA_NAV.ARBEIDSSTED"))
                .varselId(rs.getString("varselid"))
                .kontaktpersonData(kontaktpersonData)
                .soknadsstatus(soknadStatus == null ? null : EnumUtils.valueOf(Soknadsstatus.class, soknadStatus))
                .livslopsStatus(EnumUtils.valueOf(LivslopsStatus.class, rs.getString("livslopsstatus")))
                .detaljer(rs.getString("STILLING_FRA_NAV.DETALJER"))
                .build();
    }

    private static EksternAktivitetData mapEksternAktivitetData(ResultSet resultSet) throws SQLException {
        final var rs = new VeilarbAktivitetResultSet(resultSet);
        var arenaId = rs.getString("ARENA_ID");
        return new EksternAktivitetData(
            rs.getString("SOURCE"),
            rs.getString("TILTAK_KODE"),
            rs.getBoolean("OPPRETTET_SOM_HISTORISK"),
            Database.hentLocalDateTime(rs, "OPPFOLGINGSPERIODE_SLUTT"),
            arenaId != null ? new ArenaId(arenaId) : null,
            AktivitetskortType.Companion.tilAktivitetskortType(rs.getString("AKTIVITETKORT_TYPE")),
            Database.hentObjectFromJsonString(rs, "OPPGAVE", Oppgaver.class),
            Database.hentListObjectFromJsonString(rs, "HANDLINGER", LenkeSeksjon.class),
            Database.hentListObjectFromJsonString(rs, "EKSTERNAKTIVITET.DETALJER", Attributt.class),
            Database.hentListObjectFromJsonString(rs, "ETIKETTER", Etikett.class),
            Database.hentZonedDateTime(rs, "ENDRET_TIDSPUNKT_KILDE")
        );
    }
}
