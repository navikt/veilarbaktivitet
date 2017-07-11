package no.nav.fo.veilarbaktivitet.db.rowmappers;

import lombok.val;
import no.nav.fo.veilarbaktivitet.domain.*;

import java.sql.ResultSet;
import java.sql.SQLException;

import static no.nav.fo.veilarbaktivitet.db.Database.hentDato;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.*;
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
                .historiskDato(hentDato(rs, "historisk_dato"));

        if (EGENAKTIVITET.equals(type)) {
            aktivitet.egenAktivitetData(mapEgenAktivitet(rs));
        } else if (JOBBSOEKING.equals(type)) {
            aktivitet.stillingsSoekAktivitetData(mapStillingsAktivitet(rs));
        } else if (SOKEAVTALE.equals(type)) {
            aktivitet.sokeAvtaleAktivitetData(mapSokeAvtaleAktivitet(rs));
        } else if (IJOBB.equals(type)) {
            aktivitet.iJobbAktivitetData(mapIJobbAktivitet(rs));
        } else if (BEHANDLING.equals(type)) {
            aktivitet.behandlingAktivitetData(mapBehandlingAktivitet(rs));
        }

        return aktivitet.build();
    }

    private static StillingsoekAktivitetData mapStillingsAktivitet(ResultSet rs) throws SQLException {
        return new StillingsoekAktivitetData()
                .setStillingsTittel(rs.getString("stillingstittel"))
                .setArbeidsgiver(rs.getString("arbeidsgiver"))
                .setArbeidssted(rs.getString("arbeidssted"))
                .setKontaktPerson(rs.getString("kontaktperson"))
                .setStillingsoekEtikett(valueOf(StillingsoekEtikettData.class, rs.getString("etikett"))
                );
    }

    private static EgenAktivitetData mapEgenAktivitet(ResultSet rs) throws SQLException {
        return new EgenAktivitetData()
                .setHensikt(rs.getString("hensikt"))
                .setOppfolging(rs.getString("oppfolging"));
    }

    private static SokeAvtaleAktivitetData mapSokeAvtaleAktivitet(ResultSet rs) throws SQLException {
        return new SokeAvtaleAktivitetData()
                .setAntallStillingerSokes(rs.getLong("antall_stillinger_sokes"))
                .setAvtaleOppfolging(rs.getString("avtale_oppfolging"));
    }

    private static IJobbAktivitetData mapIJobbAktivitet(ResultSet rs) throws SQLException {
        return new IJobbAktivitetData()
                .setJobbStatusType(valueOf(JobbStatusTypeData.class, rs.getString("jobb_status")))
                .setAnsettelsesforhold(rs.getString("ansettelsesforhold"))
                .setArbeidstid(rs.getString("arbeidstid"));
    }

    private static BehandlingAktivitetData mapBehandlingAktivitet(ResultSet rs) throws SQLException {
        return new BehandlingAktivitetData()
                .setBehandlingType(rs.getString("behandling_type"))
                .setBehandlingSted(rs.getString("behandling_sted"))
                .setEffekt(rs.getString("effekt"))
                .setBehandlingOppfolging(rs.getString("behandling_oppfolging"));
    }

}
