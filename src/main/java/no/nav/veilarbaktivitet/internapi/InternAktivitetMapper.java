package no.nav.veilarbaktivitet.internapi;

import no.nav.veilarbaktivitet.aktivitet.domain.*;
import no.nav.veilarbaktivitet.internapi.model.*;
import no.nav.veilarbaktivitet.internapi.model.Aktivitet.AktivitetBuilder;
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

        AktivitetBuilder<?, ?> builder = switch (aktivitetType) {
            case EGENAKTIVITET -> mapTilEgenaktivitet(aktivitetData);
            case JOBBSOEKING -> mapTilJobbsoeking(aktivitetData);
            case SOKEAVTALE -> mapTilSokeavtale(aktivitetData);
            case IJOBB -> mapTilIjobb(aktivitetData);
            case BEHANDLING -> mapTilBehandling(aktivitetData);
            case MOTE -> mapTilMote(aktivitetData);
            case SAMTALEREFERAT -> mapTilSamtalereferat(aktivitetData);
            case STILLING_FRA_NAV -> mapTilStillingFraNav(aktivitetData);
        };

        return builder
                .avtaltMedNav(aktivitetData.isAvtalt())
                .aktivitetId(aktivitetData.getId().toString())
                .kontorsperreEnhetId(aktivitetData.getKontorsperreEnhetId())
                .oppfolgingsperiodeId(aktivitetData.getOppfolgingsperiodeId())
                .status(Aktivitet.StatusEnum.valueOf(aktivitetData.getStatus().name()))
                .beskrivelse(aktivitetData.getBeskrivelse())
                .tittel(aktivitetData.getTittel())
                .fraDato(toOffsetDateTime(aktivitetData.getFraDato()))
                .tilDato(toOffsetDateTime(aktivitetData.getTilDato()))
                .opprettetDato(toOffsetDateTime(aktivitetData.getOpprettetDato()))
                .endretDato(toOffsetDateTime(aktivitetData.getOpprettetDato()))
                .build();
    }

    private static AktivitetBuilder<?, ?> mapTilEgenaktivitet(AktivitetData aktivitetData) {
        EgenAktivitetData egenAktivitetData = aktivitetData.getEgenAktivitetData();

        return Egenaktivitet.builder()
                .aktivitetType(AktivitetTypeEnum.EGENAKTIVITET)
                .hensikt(egenAktivitetData.getHensikt())
                .oppfolging(egenAktivitetData.getOppfolging());
    }

    private static AktivitetBuilder<?, ?> mapTilJobbsoeking(AktivitetData aktivitetData) {
        StillingsoekAktivitetData stillingsSoekAktivitetData = aktivitetData.getStillingsSoekAktivitetData();

        Optional<StillingsoekEtikettData> stillingsoekEtikett = Optional.ofNullable(stillingsSoekAktivitetData.getStillingsoekEtikett());
        return Jobbsoeking.builder()
                .aktivitetType(AktivitetTypeEnum.JOBBSOEKING)
                .arbeidsgiver(stillingsSoekAktivitetData.getArbeidsgiver())
                .stillingsTittel(stillingsSoekAktivitetData.getStillingsTittel())
                .arbeidssted(stillingsSoekAktivitetData.getArbeidssted())
                .stillingsoekEtikett(stillingsoekEtikett.map(Enum::name).map(Jobbsoeking.StillingsoekEtikettEnum::valueOf).orElse(null))
                .kontaktPerson(stillingsSoekAktivitetData.getKontaktPerson());
    }

    private static AktivitetBuilder<?, ?> mapTilSokeavtale(AktivitetData aktivitetData) {
        SokeAvtaleAktivitetData sokeAvtaleAktivitetData = aktivitetData.getSokeAvtaleAktivitetData();

        return Sokeavtale.builder()
                .aktivitetType(AktivitetTypeEnum.SOKEAVTALE)
                .antallStillingerSokes(sokeAvtaleAktivitetData.getAntallStillingerSokes())
                .antallStillingerIUken(sokeAvtaleAktivitetData.getAntallStillingerIUken())
                .avtaleOppfolging(sokeAvtaleAktivitetData.getAvtaleOppfolging());
    }

    private static AktivitetBuilder<?, ?> mapTilIjobb(AktivitetData aktivitetData) {
        IJobbAktivitetData iJobbAktivitetData = aktivitetData.getIJobbAktivitetData();

        Optional<JobbStatusTypeData> jobbStatusType = Optional.ofNullable(iJobbAktivitetData.getJobbStatusType());
        return Ijobb.builder()
                .aktivitetType(AktivitetTypeEnum.IJOBB)
                .jobbStatusType(jobbStatusType.map(Enum::name).map(Ijobb.JobbStatusTypeEnum::valueOf).orElse(null))
                .ansettelsesforhold(iJobbAktivitetData.getAnsettelsesforhold())
                .arbeidstid(iJobbAktivitetData.getArbeidstid());
    }

    private static AktivitetBuilder<?, ?> mapTilBehandling(AktivitetData aktivitetData) {
        BehandlingAktivitetData behandlingAktivitetData = aktivitetData.getBehandlingAktivitetData();

        return Behandling.builder()
                .aktivitetType(AktivitetTypeEnum.BEHANDLING)
                .behandlingType(behandlingAktivitetData.getBehandlingType())
                .behandlingSted(behandlingAktivitetData.getBehandlingSted())
                .effekt(behandlingAktivitetData.getEffekt())
                .behandlingOppfolging(behandlingAktivitetData.getBehandlingOppfolging());
    }

    private static AktivitetBuilder<?, ?> mapTilMote(AktivitetData aktivitetData) {
        MoteData moteData = aktivitetData.getMoteData();

        return Mote.builder()
                .aktivitetType(AktivitetTypeEnum.MOTE)
                .adresse(moteData.getAdresse())
                .forberedelser(moteData.getForberedelser())
                .kanal(Mote.KanalEnum.valueOf(moteData.getKanal().name()))
                .referat(moteData.getReferat())
                .referatPublisert(moteData.isReferatPublisert());
    }

    private static AktivitetBuilder<?, ?> mapTilSamtalereferat(AktivitetData aktivitetData) {
        MoteData moteData = aktivitetData.getMoteData();

        return Samtalereferat.builder()
                .aktivitetType(AktivitetTypeEnum.SAMTALEREFERAT)
                .kanal(Samtalereferat.KanalEnum.valueOf(moteData.getKanal().name()))
                .referat(moteData.getReferat())
                .referatPublisert(moteData.isReferatPublisert());
    }

    private static AktivitetBuilder<?, ?> mapTilStillingFraNav(AktivitetData aktivitetData) {
        StillingFraNavData stillingFraNavData = aktivitetData.getStillingFraNavData();
        StillingFraNavAllOfCvKanDelesData stillingFraNavCvKanDelesData = mapCvKanDelesData(stillingFraNavData.getCvKanDelesData());

        StillingFraNav.SoknadsstatusEnum soknadsstatusEnum = Optional.ofNullable(stillingFraNavData.getSoknadsstatus())
                .map(Enum::name)
                .map(StillingFraNav.SoknadsstatusEnum::fromValue)
                .orElse(null);

        return StillingFraNav.builder()
                .aktivitetType(AktivitetTypeEnum.STILLING_FRA_NAV)
                .cvKanDelesData(stillingFraNavCvKanDelesData)
                .soknadsfrist(stillingFraNavData.getSoknadsfrist())
                .svarfrist(toLocalDate(stillingFraNavData.getSvarfrist()))
                .arbeidsgiver(stillingFraNavData.getArbeidsgiver())
                .bestillingsId(stillingFraNavData.getBestillingsId())
                .stillingsId(stillingFraNavData.getStillingsId())
                .arbeidssted(stillingFraNavData.getArbeidssted())
                .soknadsstatus(soknadsstatusEnum);
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
}
