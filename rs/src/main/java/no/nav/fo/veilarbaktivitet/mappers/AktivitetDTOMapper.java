package no.nav.fo.veilarbaktivitet.mappers;

import lombok.val;
import no.nav.fo.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;

import static java.util.Optional.ofNullable;
import static no.nav.fo.veilarbaktivitet.mappers.Helpers.etikettMap;
import static no.nav.fo.veilarbaktivitet.mappers.Helpers.typeMap;

public class AktivitetDTOMapper {
    public static AktivitetDTO mapTilAktivitetDTO(AktivitetData aktivitet) {
        val aktivitetDTO = new AktivitetDTO()
                .setId(Long.toString(aktivitet.getId()))
                .setVersjon(Long.toString(aktivitet.getVersjon()))
                .setTittel(aktivitet.getTittel())
                .setTilDato(aktivitet.getTilDato())
                .setFraDato(aktivitet.getFraDato())
                .setStatus(aktivitet.getStatus())
                .setType(typeMap.get(aktivitet.getAktivitetType()))
                .setBeskrivelse(aktivitet.getBeskrivelse())
                .setLenke(aktivitet.getLenke())
                .setAvsluttetKommentar(aktivitet.getAvsluttetKommentar())
                .setAvtalt(aktivitet.isAvtalt())
                .setOpprettetDato(aktivitet.getOpprettetDato());

        ofNullable(aktivitet.getStillingsSoekAktivitetData())
                .ifPresent(stillingsoekAktivitetData ->
                        aktivitetDTO
                                .setEtikett(etikettMap.get(stillingsoekAktivitetData.getStillingsoekEtikett()))
                                .setKontaktperson(stillingsoekAktivitetData.getKontaktPerson())
                                .setArbeidssted(stillingsoekAktivitetData.getArbeidssted())
                                .setArbeidsgiver(stillingsoekAktivitetData.getArbeidsgiver())
                                .setStillingsTittel(stillingsoekAktivitetData.getStillingsTittel())
                );
        ofNullable(aktivitet.getEgenAktivitetData())
                .ifPresent(egenAktivitetData ->
                        aktivitetDTO
                                .setHensikt(egenAktivitetData.getHensikt())
                                .setOppfolging(egenAktivitetData.getOppfolging())
                );

        ofNullable(aktivitet.getSokeAvtaleAktivitetData())
                .ifPresent(sokeAvtaleAktivitetData ->
                        aktivitetDTO
                                .setAntall(sokeAvtaleAktivitetData.getAntall())
                                .setAvtaleOppfolging(sokeAvtaleAktivitetData.getAvtaleOppfolging())
                );

        return aktivitetDTO;
    }
}
