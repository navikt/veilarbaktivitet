package no.nav.fo.veilarbaktivitet.mappers;

import lombok.val;
import no.nav.fo.veilarbaktivitet.domain.*;

import java.util.Optional;

import static no.nav.fo.veilarbaktivitet.mappers.Helpers.*;
import static no.nav.fo.veilarbaktivitet.service.BrukerService.erEksternBruker;

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
                .lagtInnAv(erEksternBruker() ? InnsenderData.BRUKER : InnsenderData.NAV)
                .lenke(aktivitetDTO.getLenke())
                .malid(aktivitetDTO.getMalid());

        switch (aktivitetType){
            case EGENAKTIVITET:
                aktivitetData.egenAktivitetData(egenAktivitetData(aktivitetDTO));
                break;
            case JOBBSOEKING:
                aktivitetData.stillingsSoekAktivitetData(stillingsoekAktivitetData(aktivitetDTO));
                break;
            case SOKEAVTALE:
                aktivitetData.sokeAvtaleAktivitetData(sokeAvtaleAktivitetData(aktivitetDTO)
                );
                break;
            case IJOBB:
                aktivitetData.iJobbAktivitetData(iJobbAktivitetData(aktivitetDTO));
                break;
            case BEHANDLING:
                aktivitetData.behandlingAktivitetData(behandlingAktivitetData(aktivitetDTO));
                break;
            case MOTE:
            case SAMTALEREFERAT:
                aktivitetData.moteData(moteData(aktivitetDTO));
                break;
        }

        return aktivitetData.build();
    }

    private static EgenAktivitetData egenAktivitetData(AktivitetDTO aktivitetDTO) {
        return EgenAktivitetData.builder()
                .hensikt(aktivitetDTO.getHensikt())
                .oppfolging(aktivitetDTO.getOppfolging())
                .build();
    }

    private static StillingsoekAktivitetData stillingsoekAktivitetData(AktivitetDTO aktivitetDTO) {
        return StillingsoekAktivitetData.builder()
                .stillingsoekEtikett(etikettMap.getKey(aktivitetDTO.getEtikett()))
                .kontaktPerson(aktivitetDTO.getKontaktperson())
                .arbeidsgiver(aktivitetDTO.getArbeidsgiver())
                .arbeidssted(aktivitetDTO.getArbeidssted())
                .stillingsTittel(aktivitetDTO.getStillingsTittel())
                .build();
    }

    private static SokeAvtaleAktivitetData sokeAvtaleAktivitetData(AktivitetDTO aktivitetDTO) {
        return SokeAvtaleAktivitetData.builder()
                .antallStillingerSokes(aktivitetDTO.getAntallStillingerSokes())
                .antallStillingerIUken(aktivitetDTO.getAntallStillingerIUken())
                .avtaleOppfolging(aktivitetDTO.getAvtaleOppfolging())
                .build();
    }

    private static IJobbAktivitetData iJobbAktivitetData(AktivitetDTO aktivitetDTO) {
        return IJobbAktivitetData.builder()
                .jobbStatusType(jobbStatusMap.getKey(aktivitetDTO.getJobbStatus()))
                .ansettelsesforhold(aktivitetDTO.getAnsettelsesforhold())
                .arbeidstid(aktivitetDTO.getArbeidstid())
                .build();
    }

    private static BehandlingAktivitetData behandlingAktivitetData(AktivitetDTO aktivitetDTO) {
        return BehandlingAktivitetData.builder()
                .behandlingType(aktivitetDTO.getBehandlingType())
                .behandlingSted(aktivitetDTO.getBehandlingSted())
                .effekt(aktivitetDTO.getEffekt())
                .behandlingOppfolging(aktivitetDTO.getBehandlingOppfolging())
                .build();
    }

    private static MoteData moteData(AktivitetDTO aktivitetDTO) {
        return MoteData.builder()
                .adresse(aktivitetDTO.getAdresse())
                .forberedelser(aktivitetDTO.getForberedelser())
                .kanal(aktivitetDTO.getKanal())
                .referat(aktivitetDTO.getReferat())
                .referatPublisert(aktivitetDTO.isErReferatPublisert())
                .build();
    }
}
