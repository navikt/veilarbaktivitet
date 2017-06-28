package no.nav.fo.veilarbaktivitet.mappers;

import no.nav.fo.veilarbaktivitet.domain.*;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.*;

import java.util.Optional;

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
                .opprettetDato(getDate(aktivitet.getOpprettet()))
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
                new StillingsoekAktivitetData()
                        .setArbeidsgiver(stilling.getArbeidsgiver())
                        .setKontaktPerson(stilling.getKontaktperson())
                        .setArbeidssted(stilling.getArbeidssted())
                        .setStillingsoekEtikett(etikettMap.get(stilling.getEtikett()))
                        .setStillingsTittel(stilling.getStillingstittel()))
                .orElse(null);
    }

    private static EgenAktivitetData mapTilEgenAktivitetData(Egenaktivitet egenaktivitet) {
        return ofNullable(egenaktivitet)
                .map(egen ->
                        new EgenAktivitetData()
                                .setHensikt(egen.getHensikt())
                                .setOppfolging(egen.getOppfolging()))
                .orElse(null);
    }

    private static SokeAvtaleAktivitetData mapTilSokeavtaleAktivitetData(Sokeavtale sokeavtaleAktivitet) {
        return ofNullable(sokeavtaleAktivitet)
                .map(sokeavtale ->
                        new SokeAvtaleAktivitetData()
                                .setAntall(sokeavtaleAktivitet.getAntall())
                                .setAvtaleOppfolging(sokeavtaleAktivitet.getAvtaleOppfolging())
                ).orElse(null);
    }

    private static IJobbAktivitetData mapTilIJobbAktivitetData(Ijobb ijobbAktivitet) {
        return ofNullable(ijobbAktivitet).map(ijobb ->
                new IJobbAktivitetData()
                        .setJobbStatusType(jobbStatusTypeMap.get(ijobb.getJobbStatus()))
                        .setAnsttelsesforhold(ijobb.getAnsettelsesforhold())
                        .setArbeidstid(ijobb.getArbeidstid())
        ).orElse(null);
    }

    private static BehandlingAktivitetData mapTilBehandlingAktivitetData(Behandling behandlingAktivitet) {
        return ofNullable(behandlingAktivitet).map(behandling ->
                new BehandlingAktivitetData()
                        .setBehandlingSted(behandling.getBehandlingSted())
                        .setEffekt(behandling.getEffekt())
                        .setBehandlingOppfolging(behandling.getBehandlingOppfolging())
        ).orElse(null);
    }
}
