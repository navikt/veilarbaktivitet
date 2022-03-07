package no.nav.veilarbaktivitet.internapi;

import lombok.val;
import no.nav.veilarbaktivitet.aktivitet.domain.*;
import no.nav.veilarbaktivitet.internapi.model.*;
import no.nav.veilarbaktivitet.internapi.model.Aktivitet.AktivitetTypeEnum;
import no.nav.veilarbaktivitet.stilling_fra_nav.CvKanDelesData;
import no.nav.veilarbaktivitet.stilling_fra_nav.StillingFraNavData;

import java.util.Optional;

import static no.nav.veilarbaktivitet.util.DateUtils.toLocalDate;
import static no.nav.veilarbaktivitet.util.DateUtils.toOffsetDateTime;

public class InternAktivitetMapper {

    private InternAktivitetMapper() {}

    public static Aktivitet mapTilAktivitet(AktivitetData aktivitetData) {
        AktivitetTypeData aktivitetType = aktivitetData.getAktivitetType();

        Aktivitet aktivitet = Aktivitet.builder()
                .avtaltMedNav(aktivitetData.isAvtalt())
                .aktivitetId(aktivitetData.getId().toString())
                .oppfolgingsperiodeId(aktivitetData.getOppfolgingsperiodeId())
                .status(Aktivitet.StatusEnum.valueOf(aktivitetData.getStatus().name()))
                .beskrivelse(aktivitetData.getBeskrivelse())
                .tittel(aktivitetData.getTittel())
                .fraDato(toOffsetDateTime(aktivitetData.getFraDato()))
                .tilDato(toOffsetDateTime(aktivitetData.getTilDato()))
                .opprettetDato(toOffsetDateTime(aktivitetData.getOpprettetDato()))
                .endretDato(toOffsetDateTime(aktivitetData.getOpprettetDato()))
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

        Optional<StillingsoekEtikettData> stillingsoekEtikett = Optional.ofNullable(stillingsSoekAktivitetData.getStillingsoekEtikett());
        val jobbsoeking = Jobbsoeking.builder()
                .aktivitetType(AktivitetTypeEnum.JOBBSOEKING)
                .arbeidsgiver(stillingsSoekAktivitetData.getArbeidsgiver())
                .stillingsTittel(stillingsSoekAktivitetData.getStillingsTittel())
                .arbeidssted(stillingsSoekAktivitetData.getArbeidssted())
                .stillingsoekEtikett(stillingsoekEtikett.map(Enum::name).map(Jobbsoeking.StillingsoekEtikettEnum::valueOf).orElse(null))
                .kontaktPerson(stillingsSoekAktivitetData.getKontaktPerson())
                .build();

        return (Jobbsoeking) merge(aktivitet, jobbsoeking);
    }

    private static Sokeavtale mapTilSokeavtale(AktivitetData aktivitetData, Aktivitet aktivitet) {
        SokeAvtaleAktivitetData sokeAvtaleAktivitetData = aktivitetData.getSokeAvtaleAktivitetData();

        val sokeavtale = Sokeavtale.builder()
                .aktivitetType(AktivitetTypeEnum.SOKEAVTALE)
                .antallStillingerSokes(sokeAvtaleAktivitetData.getAntallStillingerSokes())
                .antallStillingerIUken(sokeAvtaleAktivitetData.getAntallStillingerIUken())
                .avtaleOppfolging(sokeAvtaleAktivitetData.getAvtaleOppfolging())
                .build();

        return (Sokeavtale) merge(aktivitet, sokeavtale);
    }

    private static Ijobb mapTilIjobb(AktivitetData aktivitetData, Aktivitet aktivitet) {
        IJobbAktivitetData iJobbAktivitetData = aktivitetData.getIJobbAktivitetData();

        Optional<JobbStatusTypeData> jobbStatusType = Optional.ofNullable(iJobbAktivitetData.getJobbStatusType());
        val ijobb = Ijobb.builder()
                .aktivitetType(AktivitetTypeEnum.IJOBB)
                .jobbStatusType(jobbStatusType.map(Enum::name).map(Ijobb.JobbStatusTypeEnum::valueOf).orElse(null))
                .ansettelsesforhold(iJobbAktivitetData.getAnsettelsesforhold())
                .arbeidstid(iJobbAktivitetData.getArbeidstid())
                .build();

        return (Ijobb) merge(aktivitet, ijobb);
    }

    private static Behandling mapTilBehandling(AktivitetData aktivitetData, Aktivitet aktivitet) {
        BehandlingAktivitetData behandlingAktivitetData = aktivitetData.getBehandlingAktivitetData();

        val behandling = Behandling.builder()
                .aktivitetType(AktivitetTypeEnum.BEHANDLING)
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
                .aktivitetType(AktivitetTypeEnum.SAMTALEREFERAT)
                .kanal(Samtalereferat.KanalEnum.valueOf(moteData.getKanal().name()))
                .referat(moteData.getReferat())
                .referatPublisert(moteData.isReferatPublisert())
                .build();

        return (Samtalereferat) merge(aktivitet, samtalereferat);
    }

    private static StillingFraNav mapTilStillingFraNav(AktivitetData aktivitetData, Aktivitet aktivitet) {
        StillingFraNavData stillingFraNavData = aktivitetData.getStillingFraNavData();
        StillingFraNavAllOfCvKanDelesData stillingFraNavCvKanDelesData = mapCvKanDelesData(stillingFraNavData.getCvKanDelesData());

        StillingFraNav.SoknadsstatusEnum soknadsstatusEnum = Optional.ofNullable(stillingFraNavData.getSoknadsstatus())
                .map(Enum::name)
                .map(StillingFraNav.SoknadsstatusEnum::fromValue)
                .orElse(null);

        StillingFraNav stillingFraNav = StillingFraNav.builder()
                .aktivitetType(AktivitetTypeEnum.STILLING_FRA_NAV)
                .cvKanDelesData(stillingFraNavCvKanDelesData)
                .soknadsfrist(stillingFraNavData.getSoknadsfrist())
                .svarfrist(toLocalDate(stillingFraNavData.getSvarfrist()))
                .arbeidsgiver(stillingFraNavData.getArbeidsgiver())
                .bestillingsId(stillingFraNavData.getBestillingsId())
                .stillingsId(stillingFraNavData.getStillingsId())
                .arbeidssted(stillingFraNavData.getArbeidssted())
                .soknadsstatus(soknadsstatusEnum)
                .build();

        return (StillingFraNav) merge(aktivitet, stillingFraNav);
    }

    private static StillingFraNavAllOfCvKanDelesData mapCvKanDelesData(CvKanDelesData cvKanDelesData) {
        if (cvKanDelesData == null) return null;

        return StillingFraNavAllOfCvKanDelesData.builder()
                .kanDeles(cvKanDelesData.getKanDeles())
                .endretTidspunkt(toOffsetDateTime(cvKanDelesData.getEndretTidspunkt()))
                .endretAv(cvKanDelesData.getEndretAv())
                .endretAvType(StillingFraNavAllOfCvKanDelesData.EndretAvTypeEnum.valueOf(cvKanDelesData.getEndretAvType().name()))
                .avtaltDato(toLocalDate(cvKanDelesData.getAvtaltDato()))
                .build();
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
