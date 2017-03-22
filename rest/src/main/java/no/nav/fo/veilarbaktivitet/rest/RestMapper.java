package no.nav.fo.veilarbaktivitet.rest;

import lombok.val;
import no.nav.fo.veilarbaktivitet.domain.*;
import no.nav.fo.veilarbaktivitet.util.EnumUtils;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;

import java.util.Optional;

import static no.nav.fo.veilarbaktivitet.util.EnumUtils.getName;
import static no.nav.fo.veilarbaktivitet.util.EnumUtils.valueOf;

class RestMapper {


    private static final BidiMap<AktivitetTypeData, AktivitetTypeDTO> typeMap =
            new DualHashBidiMap<AktivitetTypeData, AktivitetTypeDTO>() {{
                put(AktivitetTypeData.EGENAKTIVITET, AktivitetTypeDTO.EGEN);
                put(AktivitetTypeData.JOBBSOEKING, AktivitetTypeDTO.STILLING);
            }};


    static AktivitetDTO mapTilAktivitetDTO(AktivitetData aktivitet) {
        val aktivitetDTO = new AktivitetDTO()
                .setId(Long.toString(aktivitet.getId()))
                .setTittel(aktivitet.getTittel())
                .setTilDato(aktivitet.getTilDato())
                .setFraDato(aktivitet.getFraDato())
                .setStatus(getName(aktivitet.getStatus()))
                .setType(typeMap.get(aktivitet.getAktivitetType()))
                .setBeskrivelse(aktivitet.getBeskrivelse())
                .setLenke(aktivitet.getLenke())
                .setOpprettetDato(aktivitet.getOpprettetDato());

        Optional.ofNullable(aktivitet.getStillingsSoekAktivitetData())
                .ifPresent(stillingsoekAktivitetData ->
                        aktivitetDTO
                                .setEtikett(getName(stillingsoekAktivitetData.getStillingsoekEtikett()))
                                .setKontaktperson(stillingsoekAktivitetData.getKontaktPerson())
                                .setArbeidssted(stillingsoekAktivitetData.getArbeidssted())
                                .setArbeidsgiver(stillingsoekAktivitetData.getArbeidsgiver())
                                .setStillingsTittel(stillingsoekAktivitetData.getStillingsTittel())
                );
        Optional.ofNullable(aktivitet.getEgenAktivitetData())
                .ifPresent(egenAktivitetData ->
                        aktivitetDTO
                                .setHensikt(egenAktivitetData.getHensikt())
                );


        return aktivitetDTO;
    }

    static AktivitetData mapTilAktivitetData(AktivitetDTO aktivitetDTO) {
        val aktivitetData = new AktivitetData()
                .setTittel(aktivitetDTO.getTittel())
                .setFraDato(aktivitetDTO.getFraDato())
                .setTilDato(aktivitetDTO.getTilDato())
                .setBeskrivelse(aktivitetDTO.getBeskrivelse())
                .setAktivitetType(typeMap.getKey(aktivitetDTO.getType()))
                .setStatus(AktivitetStatusData.valueOf(aktivitetDTO.getStatus()))
                .setLenke(aktivitetDTO.getLenke());

        AktivitetTypeData aktivitetType = aktivitetData.getAktivitetType();
        if (aktivitetType == AktivitetTypeData.EGENAKTIVITET) {
            aktivitetData.setEgenAktivitetData(new EgenAktivitetData()
                    .setHensikt(aktivitetDTO.getHensikt())
            );
        } else if (aktivitetType == AktivitetTypeData.JOBBSOEKING) {
            aktivitetData.setStillingsSoekAktivitetData(new StillingsoekAktivitetData()
                    .setStillingsoekEtikett(valueOf(StillingsoekEtikettData.class, aktivitetDTO.getEtikett()))
                    .setKontaktPerson(aktivitetDTO.getKontaktperson())
                    .setArbeidsgiver(aktivitetDTO.getArbeidsgiver())
                    .setArbeidssted(aktivitetDTO.getArbeidssted())
                    .setStillingsTittel(aktivitetDTO.getStillingsTittel())
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

