package no.nav.fo.veilarbaktivitet.mappers;

import lombok.val;
import no.nav.fo.veilarbaktivitet.domain.*;

import static no.nav.fo.veilarbaktivitet.mappers.Helpers.*;
import static no.nav.fo.veilarbaktivitet.util.FunctionUtils.nullSafe;

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
                .setLagtInnAv(aktivitet.getLagtInnAv().name())
                .setOpprettetDato(aktivitet.getOpprettetDato())
                .setEndretDato(aktivitet.getEndretDato())
                .setHistorisk(aktivitet.getHistoriskDato() != null)
                .setTransaksjonsType(aktivitet.getTransaksjonsType())
                ;

        nullSafe(AktivitetDTOMapper::mapStillingSokData).accept(aktivitetDTO, aktivitet.getStillingsSoekAktivitetData());
        nullSafe(AktivitetDTOMapper::mapEgenAktivitetData).accept(aktivitetDTO, aktivitet.getEgenAktivitetData());
        nullSafe(AktivitetDTOMapper::mapSokeAvtaleData).accept(aktivitetDTO, aktivitet.getSokeAvtaleAktivitetData());
        nullSafe(AktivitetDTOMapper::mapIJobbData).accept(aktivitetDTO, aktivitet.getIJobbAktivitetData());
        nullSafe(AktivitetDTOMapper::mapBehandleAktivitetData).accept(aktivitetDTO, aktivitet.getBehandlingAktivitetData());
        nullSafe(AktivitetDTOMapper::mapMoteData).accept(aktivitetDTO, aktivitet.getMoteData());

        return aktivitetDTO;
    }

    private static void mapMoteData(AktivitetDTO aktivitetDTO, MoteData moteData) {
        aktivitetDTO
                .setAdresse(moteData.getAdresse())
                .setForberedelser(moteData.getForberedelser())
                .setKanal(moteData.getKanal())
                .setReferat(moteData.getReferat())
                .setErReferatPublisert(moteData.isReferatPublisert());
    }

    private static void mapBehandleAktivitetData(AktivitetDTO aktivitetDTO, BehandlingAktivitetData behandlingAktivitetData) {
        aktivitetDTO.setBehandlingType(behandlingAktivitetData.getBehandlingType())
                .setBehandlingSted(behandlingAktivitetData.getBehandlingSted())
                .setEffekt(behandlingAktivitetData.getEffekt())
                .setBehandlingOppfolging(behandlingAktivitetData.getBehandlingOppfolging());
    }

    private static void mapIJobbData(AktivitetDTO aktivitetDTO, IJobbAktivitetData iJobbAktivitetData) {
        aktivitetDTO.setJobbStatus(jobbStatusMap.get(iJobbAktivitetData.getJobbStatusType()))
                .setAnsettelsesforhold(iJobbAktivitetData.getAnsettelsesforhold())
                .setArbeidstid(iJobbAktivitetData.getArbeidstid());
    }

    private static void mapSokeAvtaleData(AktivitetDTO aktivitetDTO, SokeAvtaleAktivitetData sokeAvtaleAktivitetData) {
        aktivitetDTO
                .setAntallStillingerSokes(sokeAvtaleAktivitetData.getAntallStillingerSokes())
                .setAvtaleOppfolging(sokeAvtaleAktivitetData.getAvtaleOppfolging());
    }

    private static void mapEgenAktivitetData(AktivitetDTO aktivitetDTO, EgenAktivitetData egenAktivitetData) {
        aktivitetDTO
                .setHensikt(egenAktivitetData.getHensikt())
                .setOppfolging(egenAktivitetData.getOppfolging());
    }

    private static AktivitetDTO mapStillingSokData(AktivitetDTO aktivitetDTO, StillingsoekAktivitetData stillingsoekAktivitetData) {
        return aktivitetDTO
                .setEtikett(etikettMap.get(stillingsoekAktivitetData.getStillingsoekEtikett()))
                .setKontaktperson(stillingsoekAktivitetData.getKontaktPerson())
                .setArbeidssted(stillingsoekAktivitetData.getArbeidssted())
                .setArbeidsgiver(stillingsoekAktivitetData.getArbeidsgiver())
                .setStillingsTittel(stillingsoekAktivitetData.getStillingsTittel());
    }

}
