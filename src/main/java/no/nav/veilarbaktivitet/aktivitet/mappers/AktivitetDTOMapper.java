package no.nav.veilarbaktivitet.aktivitet.mappers;

import no.nav.veilarbaktivitet.aktivitet.domain.*;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.TiltakDTO;
import no.nav.veilarbaktivitet.avtalt_med_nav.Forhaandsorientering;
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDTO;
import no.nav.veilarbaktivitet.stilling_fra_nav.CvKanDelesData;
import no.nav.veilarbaktivitet.stilling_fra_nav.StillingFraNavData;
import no.nav.veilarbaktivitet.util.FunctionUtils;

import static java.util.Optional.ofNullable;

public class AktivitetDTOMapper {

    private AktivitetDTOMapper() {}

    public static AktivitetDTO mapTilAktivitetDTO(AktivitetData aktivitet, boolean erEkstern) {

        var aktivitetDTO = new AktivitetDTO()
                .setId(Long.toString(aktivitet.getId()))
                .setFunksjonellId(aktivitet.getFunksjonellId())
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
                .setForhaandsorientering(mapForhaandsorientering(aktivitet.getForhaandsorientering()))
                .setLagtInnAv(aktivitet.getLagtInnAv().name())
                .setOpprettetDato(aktivitet.getOpprettetDato())
                .setEndretDato(aktivitet.getEndretDato())
                .setEndretAv(erEkstern ? null : aktivitet.getEndretAv()) // null ut endretAv når bruker er ekstern
                .setHistorisk(aktivitet.getHistoriskDato() != null)
                .setTransaksjonsType(aktivitet.getTransaksjonsType())
                .setOppfolgingsperiodeId(aktivitet.getOppfolgingsperiodeId());

        FunctionUtils.nullSafe(AktivitetDTOMapper::mapStillingSokData).accept(aktivitetDTO, aktivitet.getStillingsSoekAktivitetData());
        FunctionUtils.nullSafe(AktivitetDTOMapper::mapEgenAktivitetData).accept(aktivitetDTO, aktivitet.getEgenAktivitetData());
        FunctionUtils.nullSafe(AktivitetDTOMapper::mapSokeAvtaleData).accept(aktivitetDTO, aktivitet.getSokeAvtaleAktivitetData());
        FunctionUtils.nullSafe(AktivitetDTOMapper::mapIJobbData).accept(aktivitetDTO, aktivitet.getIJobbAktivitetData());
        FunctionUtils.nullSafe(AktivitetDTOMapper::mapBehandleAktivitetData).accept(aktivitetDTO, aktivitet.getBehandlingAktivitetData());
        FunctionUtils.nullSafe(AktivitetDTOMapper::mapMoteData).accept(aktivitetDTO, aktivitet.getMoteData(), erEkstern);
        FunctionUtils.nullSafe(AktivitetDTOMapper::mapStillingFraNavData).accept(aktivitetDTO, aktivitet.getStillingFraNavData(), erEkstern);
        FunctionUtils.nullSafe(AktivitetDTOMapper::mapTiltak).accept(aktivitetDTO, aktivitet.getTiltaksaktivitetData());
        return aktivitetDTO;
    }

    private static void mapStillingFraNavData(AktivitetDTO aktivitetDTO, StillingFraNavData stillingFraNavData, boolean erEkstern) {
        CvKanDelesData cvKanDelesData = null;

        if(stillingFraNavData.getCvKanDelesData() != null) {
            cvKanDelesData = stillingFraNavData.getCvKanDelesData().withEndretAv(erEkstern ? null : stillingFraNavData.getCvKanDelesData().getEndretAv());
        }
        aktivitetDTO.setStillingFraNavData(stillingFraNavData.withCvKanDelesData(cvKanDelesData));
    }

    private static void mapMoteData(AktivitetDTO aktivitetDTO, MoteData moteData, boolean erEkstern) {
        boolean skalViseReferat = moteData.isReferatPublisert() || !erEkstern;
        aktivitetDTO
                .setAdresse(moteData.getAdresse())
                .setForberedelser(moteData.getForberedelser())
                .setKanal(moteData.getKanal())
                .setReferat(skalViseReferat ? moteData.getReferat() : null)
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

    public static ForhaandsorienteringDTO mapForhaandsorientering(Forhaandsorientering forhaandsorientering) {
        return ofNullable(forhaandsorientering)
                .map(Forhaandsorientering::toDTO)
                .orElse(null);
    }

    public static void mapTiltak(AktivitetDTO aktivitetDTO, TiltaksaktivitetData tiltak) {
        aktivitetDTO.setTiltak(new TiltakDTO(
                tiltak.tiltaksnavn(),
                tiltak.arrangornavn(),
                tiltak.deltakelseStatus(),
                tiltak.dagerPerUke(),
                tiltak.deltakelsesprosent()
        ));
    }

}
