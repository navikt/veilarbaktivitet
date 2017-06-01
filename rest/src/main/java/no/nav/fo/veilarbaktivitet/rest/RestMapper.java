package no.nav.fo.veilarbaktivitet.rest;

import lombok.val;
import no.nav.fo.veilarbaktivitet.domain.*;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;

import static java.util.Optional.ofNullable;

class RestMapper {


    private static final BidiMap<AktivitetTypeData, AktivitetTypeDTO> typeMap =
            new DualHashBidiMap<AktivitetTypeData, AktivitetTypeDTO>() {{
                put(AktivitetTypeData.EGENAKTIVITET, AktivitetTypeDTO.EGEN);
                put(AktivitetTypeData.JOBBSOEKING, AktivitetTypeDTO.STILLING);
                put(AktivitetTypeData.SOKEAVTALE, AktivitetTypeDTO.SOKEAVTALE);
            }};


    private static final BidiMap<StillingsoekEtikettData, EtikettTypeDTO> etikettMap =
            new DualHashBidiMap<StillingsoekEtikettData, EtikettTypeDTO>() {{
                put(StillingsoekEtikettData.AVSLAG, EtikettTypeDTO.AVSLAG);
                put(StillingsoekEtikettData.INNKALT_TIL_INTERVJU, EtikettTypeDTO.INNKALT_TIL_INTERVJU);
                put(StillingsoekEtikettData.JOBBTILBUD, EtikettTypeDTO.JOBBTILBUD);
                put(StillingsoekEtikettData.SOKNAD_SENDT, EtikettTypeDTO.SOKNAD_SENDT);
            }};


    static AktivitetDTO mapTilAktivitetDTO(AktivitetData aktivitet) {
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

    static AktivitetData mapTilAktivitetData(AktivitetDTO aktivitetDTO) {
        val aktivitetData = new AktivitetData()
                .setId(ofNullable(aktivitetDTO.getId())
                        .filter((id) -> !id.isEmpty())
                        .map(Long::parseLong)
                        .orElse(null))
                .setVersjon(ofNullable(aktivitetDTO.versjon).map(Long::parseLong).orElse(0L))
                .setTittel(aktivitetDTO.getTittel())
                .setFraDato(aktivitetDTO.getFraDato())
                .setTilDato(aktivitetDTO.getTilDato())
                .setBeskrivelse(aktivitetDTO.getBeskrivelse())
                .setAktivitetType(typeMap.getKey(aktivitetDTO.getType()))
                .setStatus(aktivitetDTO.getStatus())
                .setAvsluttetKommentar(aktivitetDTO.getAvsluttetKommentar())
                .setAvtalt(aktivitetDTO.isAvtalt())
                .setLenke(aktivitetDTO.getLenke());

        AktivitetTypeData aktivitetType = aktivitetData.getAktivitetType();
        if (aktivitetType == AktivitetTypeData.EGENAKTIVITET) {
            aktivitetData.setEgenAktivitetData(new EgenAktivitetData()
                    .setHensikt(aktivitetDTO.getHensikt())
                    .setOppfolging(aktivitetDTO.getOppfolging())
            );
        } else if (aktivitetType == AktivitetTypeData.JOBBSOEKING) {
            aktivitetData.setStillingsSoekAktivitetData(new StillingsoekAktivitetData()
                    .setStillingsoekEtikett(etikettMap.getKey(aktivitetDTO.getEtikett()))
                    .setKontaktPerson(aktivitetDTO.getKontaktperson())
                    .setArbeidsgiver(aktivitetDTO.getArbeidsgiver())
                    .setArbeidssted(aktivitetDTO.getArbeidssted())
                    .setStillingsTittel(aktivitetDTO.getStillingsTittel())
            );
        } else if (aktivitetType == AktivitetTypeData.SOKEAVTALE) {
            aktivitetData.setSokeAvtaleAktivitetData(new SokeAvtaleAktivitetData()
                    .setAntall(aktivitetDTO.getAntall())
                    .setAvtaleOppfolging(aktivitetDTO.getAvtaleOppfolging())
            );
        }

        return aktivitetData;
    }

    static EndringsloggDTO mapEndringsLoggDTO(EndringsloggData endringsloggData) {
        return new EndringsloggDTO()
                .setEndretAv(endringsloggData.getEndretAv())
                .setEndretDato(endringsloggData.getEndretDato())
                .setEndringsBeskrivelse(endringsloggData.getEndringsBeskrivelse());
    }
}

