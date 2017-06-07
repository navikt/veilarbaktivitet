package no.nav.fo.veilarbaktivitet.mappers;

import no.nav.fo.veilarbaktivitet.domain.*;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Aktivitet;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Egenaktivitet;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Innsender;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Stillingaktivitet;

import java.util.Optional;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetStatus.aktivitetStatus;
import static no.nav.fo.veilarbaktivitet.mappers.Helpers.etikettMap;
import static no.nav.fo.veilarbaktivitet.mappers.Helpers.innsenderMap;
import static no.nav.fo.veilarbaktivitet.util.DateUtils.getDate;

public class AktivitetDataMapper {
    public static AktivitetData mapTilAktivitetData(Aktivitet aktivitet) {
        return AktivitetData.builder()
                .id(Optional.ofNullable(aktivitet.getAktivitetId())
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
                .lagtInnAv(lagtInnAv(aktivitet))
                .lenke(aktivitet.getLenke())
                .opprettetDato(getDate(aktivitet.getOpprettet()))
                .avtalt(Optional.ofNullable(aktivitet.getAvtalt()).orElse(false))
                .avsluttetKommentar(aktivitet.getAvsluttetKommentar())
                .egenAktivitetData(mapTilEgenAktivitetData(aktivitet.getEgenAktivitet()))
                .stillingsSoekAktivitetData(mapTilStillingsoekAktivitetData(aktivitet.getStillingAktivitet()))
                .build();
    }

    private static InnsenderData lagtInnAv(Aktivitet aktivitet) {
        return of(aktivitet)
                .map(Aktivitet::getLagtInnAv)
                .map(Innsender::getType)
                .map(innsenderMap::get)
                .orElse(null); // TODO kreve lagt inn av?
    }

    private static StillingsoekAktivitetData mapTilStillingsoekAktivitetData(Stillingaktivitet stillingaktivitet) {
        return Optional.ofNullable(stillingaktivitet).map(stilling ->
                new StillingsoekAktivitetData()
                        .setArbeidsgiver(stilling.getArbeidsgiver())
                        .setKontaktPerson(stilling.getKontaktperson())
                        .setArbeidssted(stilling.getArbeidssted())
                        .setStillingsoekEtikett(etikettMap.get(stilling.getEtikett()))
                        .setStillingsTittel(stilling.getStillingstittel()))
                .orElse(null);
    }

    private static EgenAktivitetData mapTilEgenAktivitetData(Egenaktivitet egenaktivitet) {
        return Optional.ofNullable(egenaktivitet)
                .map(egen ->
                        new EgenAktivitetData()
                                .setHensikt(egen.getHensikt())
                                .setOppfolging(egen.getOppfolging()))
                .orElse(null);
    }
}
