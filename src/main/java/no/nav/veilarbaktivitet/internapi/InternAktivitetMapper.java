package no.nav.veilarbaktivitet.internapi;

import lombok.val;
import no.nav.veilarbaktivitet.aktivitet.domain.*;
import no.nav.veilarbaktivitet.internapi.model.*;
import no.nav.veilarbaktivitet.internapi.model.Aktivitet.AktivitetTypeEnum;
import no.nav.veilarbaktivitet.stilling_fra_nav.CvKanDelesData;
import no.nav.veilarbaktivitet.stilling_fra_nav.StillingFraNavData;

import java.time.ZoneOffset;

public class InternAktivitetMapper {

    private InternAktivitetMapper() {}

    public static Aktivitet mapTilAktivitet(AktivitetData aktivitetData) {
        AktivitetTypeData aktivitetType = aktivitetData.getAktivitetType();

        Aktivitet aktivitet = Aktivitet.builder()
                .avtaltMedNav(aktivitetData.isAvtalt())
                .status(Aktivitet.StatusEnum.valueOf(aktivitetData.getStatus().name()))
                .beskrivelse(aktivitetData.getBeskrivelse())
                .tittel(aktivitetData.getTittel())
                .fraDato(aktivitetData.getFraDato().toInstant().atOffset(ZoneOffset.UTC))
                .tilDato(aktivitetData.getTilDato().toInstant().atOffset(ZoneOffset.UTC))
                .opprettetDato(aktivitetData.getOpprettetDato().toInstant().atOffset(ZoneOffset.UTC))
                .endretDato(aktivitetData.getOpprettetDato().toInstant().atOffset(ZoneOffset.UTC))
                .build();

        return switch (aktivitetType) {
            case EGENAKTIVITET -> mapTilEgenaktivitet(aktivitetData, aktivitet);
            case JOBBSOEKING -> mapTilJobbsoeking(aktivitetData, aktivitet);
            case SOKEAVTALE -> mapTilSokeavtale(aktivitetData, aktivitet);
            case IJOBB -> mapTilIjobb(aktivitetData, aktivitet);
            case BEHANDLING -> mapTilBehandling(aktivitetData, aktivitet);
            case MOTE -> mapTilMote(aktivitetData, aktivitet);
            case SAMTALEREFERAT -> mapTilSamtalereferat(aktivitetData, aktivitet);
            case STILLING_FRA_NAV -> mapTilStillingFraNav(aktivitetData, aktivitet);
        };
    }

    private static Egenaktivitet mapTilEgenaktivitet(AktivitetData aktivitetData, Aktivitet aktivitet) {
        EgenAktivitetData egenAktivitetData = aktivitetData.getEgenAktivitetData();

        val egenaktivitet = Egenaktivitet.builder()
                .aktivitetType(AktivitetTypeEnum.EGENAKTIVITET)
                .hensikt(egenAktivitetData.getHensikt())
                .oppfolging(egenAktivitetData.getOppfolging())
                .build();

        return (Egenaktivitet) merge(aktivitet, egenaktivitet);
    }

    private static Jobbsoeking mapTilJobbsoeking(AktivitetData aktivitetData, Aktivitet aktivitet) {
        StillingsoekAktivitetData stillingsSoekAktivitetData = aktivitetData.getStillingsSoekAktivitetData();

        val jobbsoeking = Jobbsoeking.builder()
                .arbeidsgiver(stillingsSoekAktivitetData.getArbeidsgiver())
                .stillingsTittel(stillingsSoekAktivitetData.getStillingsTittel())
                .arbeidssted(stillingsSoekAktivitetData.getArbeidssted())
                .stillingsoekEtikett(Jobbsoeking.StillingsoekEtikettEnum.valueOf(stillingsSoekAktivitetData.getStillingsoekEtikett().name()))
                .kontaktPerson(stillingsSoekAktivitetData.getKontaktPerson())
                .build();

        return (Jobbsoeking) merge(aktivitet, jobbsoeking);
    }

    private static Sokeavtale mapTilSokeavtale(AktivitetData aktivitetData, Aktivitet aktivitet) {
        SokeAvtaleAktivitetData sokeAvtaleAktivitetData = aktivitetData.getSokeAvtaleAktivitetData();

        val sokeavtale = Sokeavtale.builder()
                .antallStillingerSokes(sokeAvtaleAktivitetData.getAntallStillingerSokes())
                .antallStillingerIUken(sokeAvtaleAktivitetData.getAntallStillingerIUken())
                .avtaleOppfolging(sokeAvtaleAktivitetData.getAvtaleOppfolging())
                .build();

        return (Sokeavtale) merge(aktivitet, sokeavtale);
    }

    private static Ijobb mapTilIjobb(AktivitetData aktivitetData, Aktivitet aktivitet) {
        IJobbAktivitetData iJobbAktivitetData = aktivitetData.getIJobbAktivitetData();

        val ijobb = Ijobb.builder()
                .jobbStatusType(Ijobb.JobbStatusTypeEnum.valueOf(iJobbAktivitetData.getJobbStatusType().name()))
                .ansettelsesforhold(iJobbAktivitetData.getAnsettelsesforhold())
                .arbeidstid(iJobbAktivitetData.getArbeidstid())
                .build();

        return (Ijobb) merge(aktivitet, ijobb);
    }

    private static Behandling mapTilBehandling(AktivitetData aktivitetData, Aktivitet aktivitet) {
        BehandlingAktivitetData behandlingAktivitetData = aktivitetData.getBehandlingAktivitetData();

        val behandling = Behandling.builder()
                .behandlingType(behandlingAktivitetData.getBehandlingType())
                .behandlingSted(behandlingAktivitetData.getBehandlingSted())
                .effekt(behandlingAktivitetData.getEffekt())
                .behandlingOppfolging(behandlingAktivitetData.getBehandlingOppfolging())
                .build();

        return (Behandling) merge(aktivitet, behandling);
    }

    private static Mote mapTilMote(AktivitetData aktivitetData, Aktivitet aktivitet) {
        MoteData moteData = aktivitetData.getMoteData();

        val moteaktivitet = Mote.builder()
                .aktivitetType(AktivitetTypeEnum.MOTE)
                .adresse(moteData.getAdresse())
                .forberedelser(moteData.getForberedelser())
                .kanal(Mote.KanalEnum.valueOf(moteData.getKanal().name()))
                .referat(moteData.getReferat())
                .referatPublisert(moteData.isReferatPublisert())
                .build();

        return (Mote) merge(aktivitet, moteaktivitet);
    }

    private static Samtalereferat mapTilSamtalereferat(AktivitetData aktivitetData, Aktivitet aktivitet) {
        MoteData moteData = aktivitetData.getMoteData();

        val samtalereferat = Samtalereferat.builder()
                .kanal(Samtalereferat.KanalEnum.valueOf(moteData.getKanal().name()))
                .referat(moteData.getReferat())
                .referatPublisert(moteData.isReferatPublisert())
                .build();

        return (Samtalereferat) merge(aktivitet, samtalereferat);
    }

    private static StillingFraNav mapTilStillingFraNav(AktivitetData aktivitetData, Aktivitet aktivitet) {
        StillingFraNavData stillingFraNavData = aktivitetData.getStillingFraNavData();
        CvKanDelesData cvKanDelesData = stillingFraNavData.getCvKanDelesData();

        StillingFraNavAllOfCvKanDelesData stillingFraNavCvKanDelesData = StillingFraNavAllOfCvKanDelesData.builder()
                .kanDeles(cvKanDelesData.getKanDeles())
                .endretTidspunkt(cvKanDelesData.getEndretTidspunkt().toInstant().atOffset(ZoneOffset.UTC))
                .endretAv(cvKanDelesData.getEndretAv())
                .endretAvType(StillingFraNavAllOfCvKanDelesData.EndretAvTypeEnum.valueOf(cvKanDelesData.getEndretAvType().name()))
                .avtaltDato(cvKanDelesData.getAvtaltDato().toInstant().atOffset(ZoneOffset.UTC).toLocalDate())
                .build();

        StillingFraNav stillingFraNav = StillingFraNav.builder()
                .cvKanDelesData(stillingFraNavCvKanDelesData)
                .soknadsfrist(stillingFraNavData.getSoknadsfrist())
                .svarfrist(stillingFraNavData.getSvarfrist().toInstant().atOffset(ZoneOffset.UTC).toLocalDate())
                .arbeidsgiver(stillingFraNavData.getArbeidsgiver())
                .bestillingsId(stillingFraNavData.getBestillingsId())
                .stillingsId(stillingFraNavData.getStillingsId())
                .arbeidssted(stillingFraNavData.getArbeidssted())
                .soknadsstatus(StillingFraNav.SoknadsstatusEnum.valueOf(stillingFraNavData.getSoknadsstatus().name()))
                .build();

        return (StillingFraNav) merge(aktivitet, stillingFraNav);
    }

    private static Aktivitet merge(Aktivitet base, Aktivitet aktivitet) {
        return aktivitet
                .avtaltMedNav(base.getAvtaltMedNav())
                .status(base.getStatus())
                .beskrivelse(base.getBeskrivelse())
                .tittel(base.getTittel())
                .fraDato(base.getFraDato())
                .tilDato(base.getTilDato())
                .opprettetDato(base.getOpprettetDato())
                .endretDato(base.getOpprettetDato());
    }
}
