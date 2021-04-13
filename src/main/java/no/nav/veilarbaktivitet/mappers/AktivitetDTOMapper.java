package no.nav.veilarbaktivitet.mappers;

import lombok.val;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.veilarbaktivitet.domain.*;
import no.nav.veilarbaktivitet.util.FunctionUtils;

public class AktivitetDTOMapper {

    public static AktivitetDTO mapTilAktivitetDTO(AktivitetData aktivitet) {
        val aktivitetDTO = new AktivitetDTO()
                .setId(Long.toString(aktivitet.getId()))
                .setVersjon(Long.toString(aktivitet.getVersjon()))
                .setTittel(aktivitet.getTittel())
                .setTilDato(aktivitet.getTilDato())
                .setFraDato(aktivitet.getFraDato())
                .setStatus(aktivitet.getStatus())
                .setType(Helpers.Type.getDTO(aktivitet.getAktivitetType()))
                .setBeskrivelse(aktivitet.getBeskrivelse())
                .setLenke(aktivitet.getLenke())
                .setAvsluttetKommentar(aktivitet.getAvsluttetKommentar())
                .setAvtalt(aktivitet.isAvtalt())
                .setForhaandsorientering(aktivitet.getForhaandsorientering())
                .setLagtInnAv(aktivitet.getLagtInnAv().name())
                .setOpprettetDato(aktivitet.getOpprettetDato())
                .setEndretDato(aktivitet.getEndretDato())
                .setHistorisk(aktivitet.getHistoriskDato() != null)
                .setTransaksjonsType(aktivitet.getTransaksjonsType());

        // TODO: Ikke bruk statiske ting som dette inne i en mapper
        if (AuthContextHolderThreadLocal.instance().erInternBruker()) {
            aktivitetDTO.setEndretAv(aktivitet.getEndretAv());
        }

        FunctionUtils.nullSafe(AktivitetDTOMapper::mapStillingSokData).accept(aktivitetDTO, aktivitet.getStillingsSoekAktivitetData());
        FunctionUtils.nullSafe(AktivitetDTOMapper::mapEgenAktivitetData).accept(aktivitetDTO, aktivitet.getEgenAktivitetData());
        FunctionUtils.nullSafe(AktivitetDTOMapper::mapSokeAvtaleData).accept(aktivitetDTO, aktivitet.getSokeAvtaleAktivitetData());
        FunctionUtils.nullSafe(AktivitetDTOMapper::mapIJobbData).accept(aktivitetDTO, aktivitet.getIJobbAktivitetData());
        FunctionUtils.nullSafe(AktivitetDTOMapper::mapBehandleAktivitetData).accept(aktivitetDTO, aktivitet.getBehandlingAktivitetData());
        FunctionUtils.nullSafe(AktivitetDTOMapper::mapMoteData).accept(aktivitetDTO, aktivitet.getMoteData());

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
        aktivitetDTO.setJobbStatus(Helpers.JobbStatus.getDTO(iJobbAktivitetData.getJobbStatusType()))
                .setAnsettelsesforhold(iJobbAktivitetData.getAnsettelsesforhold())
                .setArbeidstid(iJobbAktivitetData.getArbeidstid());
    }

    private static void mapSokeAvtaleData(AktivitetDTO aktivitetDTO, SokeAvtaleAktivitetData sokeAvtaleAktivitetData) {
        aktivitetDTO
                .setAntallStillingerSokes(sokeAvtaleAktivitetData.getAntallStillingerSokes())
                .setAntallStillingerIUken(sokeAvtaleAktivitetData.getAntallStillingerIUken())
                .setAvtaleOppfolging(sokeAvtaleAktivitetData.getAvtaleOppfolging());
    }

    private static void mapEgenAktivitetData(AktivitetDTO aktivitetDTO, EgenAktivitetData egenAktivitetData) {
        aktivitetDTO
                .setHensikt(egenAktivitetData.getHensikt())
                .setOppfolging(egenAktivitetData.getOppfolging());
    }

    private static AktivitetDTO mapStillingSokData(AktivitetDTO aktivitetDTO, StillingsoekAktivitetData stillingsoekAktivitetData) {
        return aktivitetDTO
                .setEtikett(Helpers.Etikett.getDTO(stillingsoekAktivitetData.getStillingsoekEtikett()))
                .setKontaktperson(stillingsoekAktivitetData.getKontaktPerson())
                .setArbeidssted(stillingsoekAktivitetData.getArbeidssted())
                .setArbeidsgiver(stillingsoekAktivitetData.getArbeidsgiver())
                .setStillingsTittel(stillingsoekAktivitetData.getStillingsTittel());
    }

}
