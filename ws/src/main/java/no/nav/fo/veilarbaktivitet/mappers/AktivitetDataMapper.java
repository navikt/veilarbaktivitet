package no.nav.fo.veilarbaktivitet.mappers;

import no.nav.fo.veilarbaktivitet.domain.*;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.*;

import static java.util.Optional.ofNullable;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetStatus.aktivitetStatus;
import static no.nav.fo.veilarbaktivitet.mappers.Helpers.etikettMap;
import static no.nav.fo.veilarbaktivitet.mappers.Helpers.jobbStatusTypeMap;
import static no.nav.fo.veilarbaktivitet.util.DateUtils.getDate;

public class AktivitetDataMapper {
    public static AktivitetData mapTilAktivitetData(Aktivitet aktivitet) {
        return AktivitetData.builder()
                .id(ofNullable(aktivitet.getAktivitetId())
                        .filter((id) -> !id.isEmpty())
                        .map(Long::parseLong)
                        .orElse(null)
                )
                .versjon(ofNullable(aktivitet.getVersjon())
                        .filter((versjon) -> !versjon.isEmpty())
                        .map(Long::parseLong)
                        .orElse(0L)
                )
                .tittel(aktivitet.getTittel())
                .fraDato(getDate(aktivitet.getFom()))
                .tilDato(getDate(aktivitet.getTom()))
                .beskrivelse(aktivitet.getBeskrivelse())
                .aktivitetType(AktivitetTypeData.valueOf(aktivitet.getType().name()))
                .status(aktivitetStatus(aktivitet.getStatus()))
                .lagtInnAv(InnsenderData.BRUKER) // vet at fra denne siden er det alltid BRUKER
                .lenke(aktivitet.getLenke())
                .opprettetDato(getDate(aktivitet.getOpprettet())) // note: not used!! so don't worry about it
                .avtalt(ofNullable(aktivitet.getAvtalt()).orElse(false))
                .avsluttetKommentar(aktivitet.getAvsluttetKommentar())
                .egenAktivitetData(mapTilEgenAktivitetData(aktivitet.getEgenAktivitet()))
                .stillingsSoekAktivitetData(mapTilStillingsoekAktivitetData(aktivitet.getStillingAktivitet()))
                .sokeAvtaleAktivitetData(mapTilSokeavtaleAktivitetData(aktivitet.getSokeavtale()))
                .iJobbAktivitetData(mapTilIJobbAktivitetData(aktivitet.getIjobb()))
                .behandlingAktivitetData(mapTilBehandlingAktivitetData(aktivitet.getBehandling()))
                .build();
    }

    private static StillingsoekAktivitetData mapTilStillingsoekAktivitetData(Stillingaktivitet stillingaktivitet) {
        return ofNullable(stillingaktivitet).map(stilling ->
                StillingsoekAktivitetData.builder()
                        .arbeidsgiver(stilling.getArbeidsgiver())
                        .kontaktPerson(stilling.getKontaktperson())
                        .arbeidssted(stilling.getArbeidssted())
                        .stillingsoekEtikett(etikettMap.get(stilling.getEtikett()))
                        .stillingsTittel(stilling.getStillingstittel())
                        .build()
        ).orElse(null);
    }

    private static EgenAktivitetData mapTilEgenAktivitetData(Egenaktivitet egenaktivitet) {
        return ofNullable(egenaktivitet)
                .map(egen ->
                        EgenAktivitetData.builder()
                                .hensikt(egen.getHensikt())
                                .oppfolging(egen.getOppfolging())
                                .build()
                ).orElse(null);
    }

    private static SokeAvtaleAktivitetData mapTilSokeavtaleAktivitetData(Sokeavtale sokeavtaleAktivitet) {
        return ofNullable(sokeavtaleAktivitet)
                .map(sokeavtale ->
                        SokeAvtaleAktivitetData.builder()
                                .antallStillingerSokes(sokeavtaleAktivitet.getAntall())
                                .avtaleOppfolging(sokeavtaleAktivitet.getAvtaleOppfolging())
                                .build()
                ).orElse(null);
    }

    private static IJobbAktivitetData mapTilIJobbAktivitetData(Ijobb ijobbAktivitet) {
        return ofNullable(ijobbAktivitet).map(ijobb ->
                IJobbAktivitetData.builder()
                        .jobbStatusType(jobbStatusTypeMap.get(ijobb.getJobbStatus()))
                        .ansettelsesforhold(ijobb.getAnsettelsesforhold())
                        .arbeidstid(ijobb.getArbeidstid())
                        .build()
        ).orElse(null);
    }

    private static BehandlingAktivitetData mapTilBehandlingAktivitetData(Behandling behandlingAktivitet) {
        return ofNullable(behandlingAktivitet).map(behandling ->
                BehandlingAktivitetData.builder()
                        .behandlingType(behandling.getBehandlingType())
                        .behandlingSted(behandling.getBehandlingSted())
                        .effekt(behandling.getEffekt())
                        .behandlingOppfolging(behandling.getBehandlingOppfolging())
                        .build()
        ).orElse(null);
    }
}
