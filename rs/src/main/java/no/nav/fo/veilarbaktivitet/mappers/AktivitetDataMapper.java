package no.nav.fo.veilarbaktivitet.mappers;

import lombok.val;
import no.nav.fo.veilarbaktivitet.domain.*;

import java.util.Optional;

import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.*;
import static no.nav.fo.veilarbaktivitet.mappers.Helpers.*;

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
                .lagtInnAv(InnsenderData.NAV) // Vet at det alltid er fra NAV på denne siden
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
        } else if (IJOBB.equals(aktivitetType)) {
            aktivitetData.iJobbAktivitetData(new IJobbAktivitetData()
                    .setJobbStatusType(jobbStatusMap.getKey(aktivitetDTO.getJobbStatus()))
                    .setAnsttelsesforhold(aktivitetDTO.getAnsettelsesforhold())
                    .setArbeidstid(aktivitetDTO.getArbeidstid())
            );
        } else if (BEHANDLING.equals(aktivitetType)) {
            aktivitetData.behandlingAktivitetData(new BehandlingAktivitetData()
                    .setBehandlingSted(aktivitetDTO.getBehandlingSted())
                    .setEffekt(aktivitetDTO.getEffekt())
                    .setBehandlingOppfolging(aktivitetDTO.getBehandlingOppfolging())
            );
        }

        return aktivitetData.build();
    }
}
