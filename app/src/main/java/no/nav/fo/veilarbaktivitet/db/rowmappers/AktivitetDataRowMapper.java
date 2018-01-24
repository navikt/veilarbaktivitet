package no.nav.fo.veilarbaktivitet.db.rowmappers;

import lombok.val;
import no.nav.fo.veilarbaktivitet.domain.*;

import java.sql.ResultSet;
import java.sql.SQLException;

import static no.nav.apiapp.util.EnumUtils.valueOfOptional;
import static no.nav.fo.veilarbaktivitet.db.Database.hentDato;
import static no.nav.fo.veilarbaktivitet.util.EnumUtils.valueOf;

public class AktivitetDataRowMapper {
    public static AktivitetData mapAktivitet(ResultSet rs) throws SQLException {
        val type = AktivitetTypeData.valueOf(rs.getString("aktivitet_type_kode"));
        val aktivitet = AktivitetData
                .builder()
                .id(rs.getLong("aktivitet_id"))
                .versjon(rs.getLong("versjon"))
                .aktorId(rs.getString("aktor_id"))
                .aktivitetType(type)
                .fraDato(hentDato(rs, "fra_dato"))
                .tilDato(hentDato(rs, "til_dato"))
                .tittel(rs.getString("tittel"))
                .beskrivelse(rs.getString("beskrivelse"))
                .status(valueOf(AktivitetStatus.class, rs.getString("livslopstatus_kode")))
                .avsluttetKommentar(rs.getString("avsluttet_kommentar"))
                .opprettetDato(hentDato(rs, "opprettet_dato"))
                .endretDato(hentDato(rs, "endret_dato"))
                .lagtInnAv(valueOf(InnsenderData.class, rs.getString("lagt_inn_av")))
                .avtalt(rs.getBoolean("avtalt"))
                .lenke(rs.getString("lenke"))
                .transaksjonsType(
                        valueOf(AktivitetTransaksjonsType.class,
                                rs.getString("transaksjons_type"))
                )
                .historiskDato(hentDato(rs, "historisk_dato"))
                .kontorsperreEnhetId(rs.getString("kontorsperre_enhet_id"));

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
            case MOTE:
            case SAMTALEREFERAT:
                aktivitet.moteData(mapMoteData(rs));
                break;
        }

        return aktivitet.build();
    }

    private static MoteData mapMoteData(ResultSet rs) throws SQLException {
        return MoteData.builder()
                .adresse(rs.getString("adresse"))
                .forberedelser(rs.getString("forberedelser"))
                .kanal(valueOfOptional(KanalDTO.class, rs.getString("kanal")).orElse(null))
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
                .stillingsoekEtikett(valueOf(StillingsoekEtikettData.class, rs.getString("etikett")))
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
                .avtaleOppfolging(rs.getString("avtale_oppfolging"))
                .build();
    }

    private static IJobbAktivitetData mapIJobbAktivitet(ResultSet rs) throws SQLException {
        return IJobbAktivitetData.builder()
                .jobbStatusType(valueOf(JobbStatusTypeData.class, rs.getString("jobb_status")))
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

}
