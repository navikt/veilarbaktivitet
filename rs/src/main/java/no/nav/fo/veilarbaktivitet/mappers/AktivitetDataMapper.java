package no.nav.fo.veilarbaktivitet.mappers;

import lombok.val;
import no.nav.fo.veilarbaktivitet.domain.*;

import java.util.Optional;

import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.EGENAKTIVITET;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.JOBBSOEKING;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.SOKEAVTALE;
import static no.nav.fo.veilarbaktivitet.mappers.Helpers.etikettMap;
import static no.nav.fo.veilarbaktivitet.mappers.Helpers.typeMap;

public class AktivitetDataMapper {
    public static AktivitetData mapTilAktivitetData(AktivitetDTO aktivitetDTO) {

        val id = Optional.ofNullable(aktivitetDTO.getId())
                .filter((s) -> !s.isEmpty())
                .map(Long::parseLong)
                .orElse(null);
        val versjon = Optional.ofNullable(aktivitetDTO.versjon).map(Long::parseLong).orElse(0L);
        val aktivitetType = typeMap.getKey(aktivitetDTO.getType());

        val aktivitetData = AktivitetData
                .builder()
                .id(id)
                .versjon(versjon)
                .tittel(aktivitetDTO.getTittel())
                .fraDato(aktivitetDTO.getFraDato())
                .tilDato(aktivitetDTO.getTilDato())
                .beskrivelse(aktivitetDTO.getBeskrivelse())
                .aktivitetType(aktivitetType)
                .status(aktivitetDTO.getStatus())
                .avsluttetKommentar(aktivitetDTO.getAvsluttetKommentar())
                .avtalt(aktivitetDTO.isAvtalt())
                .lenke(aktivitetDTO.getLenke());

        if (EGENAKTIVITET.equals(aktivitetType)) {
            aktivitetData.egenAktivitetData(new EgenAktivitetData()
                    .setHensikt(aktivitetDTO.getHensikt())
                    .setOppfolging(aktivitetDTO.getOppfolging())
            );
        } else if (JOBBSOEKING.equals(aktivitetType)) {
            aktivitetData.stillingsSoekAktivitetData(new StillingsoekAktivitetData()
                    .setStillingsoekEtikett(etikettMap.getKey(aktivitetDTO.getEtikett()))
                    .setKontaktPerson(aktivitetDTO.getKontaktperson())
                    .setArbeidsgiver(aktivitetDTO.getArbeidsgiver())
                    .setArbeidssted(aktivitetDTO.getArbeidssted())
                    .setStillingsTittel(aktivitetDTO.getStillingsTittel())
            );
        } else if (SOKEAVTALE.equals(aktivitetType)) {
            aktivitetData.sokeAvtaleAktivitetData(new SokeAvtaleAktivitetData()
                    .setAntall(aktivitetDTO.getAntall())
                    .setAvtaleOppfolging(aktivitetDTO.getAvtaleOppfolging())
            );
        }

        return aktivitetData.build();
    }
}
