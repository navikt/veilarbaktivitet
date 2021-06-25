package no.nav.veilarbaktivitet.mappers;

import no.nav.veilarbaktivitet.avtaltMedNav.Forhaandsorientering;
import no.nav.veilarbaktivitet.avtaltMedNav.ForhaandsorienteringDTO;
import no.nav.veilarbaktivitet.avtaltMedNav.Type;
import no.nav.veilarbaktivitet.domain.*;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;


public class AktivitetDTOMapperTest {

    private static final Random random = new Random();

    @Test
    public void skalMappeAktivitetsFelter() {
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyAktivitet()
                .aktivitetType(AktivitetTypeData.MOTE)
                .build();

        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);

        SoftAssertions.assertSoftly( s -> {
                    s.assertThat(aktivitetDTO.getId()).isEqualTo(aktivitetData.getId().toString());
                    s.assertThat(aktivitetDTO.getVersjon()).isEqualTo(aktivitetData.getVersjon().toString());
                    s.assertThat(aktivitetDTO.getTittel()).isEqualTo(aktivitetData.getTittel());
                    s.assertThat(aktivitetDTO.getTilDato()).isEqualTo(aktivitetData.getTilDato());
                    s.assertThat(aktivitetDTO.getFraDato()).isEqualTo(aktivitetData.getFraDato());
                    s.assertThat(aktivitetDTO.getStatus()).isEqualTo(aktivitetData.getStatus());
                    s.assertThat(aktivitetDTO.getType()).isEqualTo(Helpers.Type.getDTO(aktivitetData.getAktivitetType()));
                    s.assertThat(aktivitetDTO.getBeskrivelse()).isEqualTo(aktivitetData.getBeskrivelse());
                    s.assertThat(aktivitetDTO.getLenke()).isEqualTo(aktivitetData.getLenke());
                    s.assertThat(aktivitetDTO.getAvsluttetKommentar()).isEqualTo(aktivitetData.getAvsluttetKommentar());
                    s.assertThat(aktivitetDTO.isAvtalt()).isEqualTo(aktivitetData.isAvtalt());
                    s.assertThat(aktivitetDTO.getLagtInnAv()).isEqualTo(aktivitetData.getLagtInnAv().name());
                    s.assertThat(aktivitetDTO.getOpprettetDato()).isEqualTo(aktivitetData.getOpprettetDato());
                    s.assertThat(aktivitetDTO.getEndretDato()).isEqualTo(aktivitetData.getEndretDato());
                    s.assertThat(aktivitetDTO.getEndretAv()).isEqualTo(aktivitetData.getEndretAv());
                    if (aktivitetDTO.isHistorisk()) {
                        s.assertThat(aktivitetData.getHistoriskDato()).isNotNull();
                    } else {
                        s.assertThat(aktivitetData.getHistoriskDato()).isNull();
                    }
                    s.assertThat(aktivitetDTO.getTransaksjonsType()).isEqualTo(aktivitetData.getTransaksjonsType());
                    s.assertAll();
                });
    }

    @Test
    public void skalMappeForhaandsorientering() {
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyAktivitet()
                .aktivitetType(AktivitetTypeData.MOTE)
                .forhaandsorientering(Forhaandsorientering
                        .builder()
                        .id("1234")
                        .type(Type.SEND_FORHAANDSORIENTERING)
                        .tekst("Forhåndsorientering")
                        .lestDato(AktivitetDataTestBuilder.nyDato())
                        .build())
                .build();

        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        SoftAssertions.assertSoftly( s -> {
            s.assertThat(aktivitetDTO.getType()).isEqualTo(Helpers.Type.getDTO(aktivitetData.getAktivitetType()));
            ForhaandsorienteringDTO mappingResult = aktivitetDTO.getForhaandsorientering();
            Forhaandsorientering mappingSource = aktivitetData.getForhaandsorientering();
            s.assertThat(mappingResult).isNotNull();
            s.assertThat(mappingResult.getId()).isEqualTo(mappingSource.getId());
            s.assertThat(mappingResult.getType()).isEqualTo(mappingSource.getType());
            s.assertThat(mappingResult.getTekst()).isEqualTo(mappingSource.getTekst());
            s.assertThat(mappingResult.getLestDato()).isEqualTo(mappingSource.getLestDato());
            s.assertAll();
        });

    }

    @Test
    public void skalMappeStillingSokData() {
        AktivitetData nyttStillingsoek = AktivitetDataTestBuilder.nyttStillingssøk();
        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(nyttStillingsoek, false);
        SoftAssertions.assertSoftly( s -> {
            s.assertThat(aktivitetDTO.getType()).isEqualTo(AktivitetTypeDTO.STILLING);
            s.assertThat(aktivitetDTO.getArbeidsgiver()).isEqualTo(nyttStillingsoek.getStillingsSoekAktivitetData().getArbeidsgiver());
            s.assertThat(aktivitetDTO.getStillingsTittel()).isEqualTo(nyttStillingsoek.getStillingsSoekAktivitetData().getStillingsTittel());
            s.assertThat(aktivitetDTO.getArbeidssted()).isEqualTo(nyttStillingsoek.getStillingsSoekAktivitetData().getArbeidssted());
            s.assertThat(aktivitetDTO.getKontaktperson()).isEqualTo(nyttStillingsoek.getStillingsSoekAktivitetData().getKontaktPerson());
            s.assertThat(aktivitetDTO.getEtikett()).isEqualTo(Helpers.Etikett.getDTO(nyttStillingsoek.getStillingsSoekAktivitetData().getStillingsoekEtikett()));

            s.assertAll();
        });
    }

    @Test
    public void skalMappeEgenAktivitetData() {
        AktivitetData nyEgenaktivitet = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(nyEgenaktivitet, false);
        SoftAssertions.assertSoftly( s -> {
            s.assertThat(aktivitetDTO.getType()).isEqualTo(AktivitetTypeDTO.EGEN);
            s.assertThat(aktivitetDTO.getHensikt()).isEqualTo(nyEgenaktivitet.getEgenAktivitetData().getHensikt());
            s.assertThat(aktivitetDTO.getOppfolging()).isEqualTo(nyEgenaktivitet.getEgenAktivitetData().getOppfolging());
            s.assertAll();
        });

    }

    @Test
    public void skalMappeSokeAvtaleData() {
        AktivitetData nySokeAvtaleAktivitet = AktivitetDataTestBuilder.nySokeAvtaleAktivitet();
        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(nySokeAvtaleAktivitet, false);
        SoftAssertions.assertSoftly( s -> {
            s.assertThat(aktivitetDTO.getType()).isEqualTo(AktivitetTypeDTO.SOKEAVTALE);
            s.assertThat(aktivitetDTO.getAntallStillingerIUken()).isEqualTo(nySokeAvtaleAktivitet.getSokeAvtaleAktivitetData().antallStillingerIUken);
            s.assertThat(aktivitetDTO.getAntallStillingerSokes()).isEqualTo(nySokeAvtaleAktivitet.getSokeAvtaleAktivitetData().antallStillingerSokes);
            s.assertThat(aktivitetDTO.getAvtaleOppfolging()).isEqualTo(nySokeAvtaleAktivitet.getSokeAvtaleAktivitetData().avtaleOppfolging);
            s.assertAll();
        });
    }

    @Test
    public void skalMappeIJobbData() {
        AktivitetData nyIJobbAktivitet = AktivitetDataTestBuilder.nyIJobbAktivitet();
        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(nyIJobbAktivitet, false);
        SoftAssertions.assertSoftly( s -> {
            s.assertThat(aktivitetDTO.getType()).isEqualTo(AktivitetTypeDTO.IJOBB);
            s.assertThat(aktivitetDTO.getJobbStatus()).isEqualTo(Helpers.JobbStatus.getDTO(nyIJobbAktivitet.getIJobbAktivitetData().getJobbStatusType()));
            s.assertThat(aktivitetDTO.getAnsettelsesforhold()).isEqualTo(nyIJobbAktivitet.getIJobbAktivitetData().getAnsettelsesforhold());
            s.assertThat(aktivitetDTO.getArbeidstid()).isEqualTo(nyIJobbAktivitet.getIJobbAktivitetData().getArbeidstid());
            s.assertAll();
        });
    }

    @Test
    public void skalMappeBehandleAktivitetData() {
        AktivitetData nyBehandlingAktivitet = AktivitetDataTestBuilder.nyBehandlingAktivitet();
        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(nyBehandlingAktivitet, false);
        SoftAssertions.assertSoftly( s -> {
            s.assertThat(aktivitetDTO.getType()).isEqualTo(AktivitetTypeDTO.BEHANDLING);
            s.assertThat(aktivitetDTO.getBehandlingType()).isEqualTo(nyBehandlingAktivitet.getBehandlingAktivitetData().getBehandlingType());
            s.assertThat(aktivitetDTO.getBehandlingSted()).isEqualTo(nyBehandlingAktivitet.getBehandlingAktivitetData().getBehandlingSted());
            s.assertThat(aktivitetDTO.getBehandlingOppfolging()).isEqualTo(nyBehandlingAktivitet.getBehandlingAktivitetData().getBehandlingOppfolging());
            s.assertThat(aktivitetDTO.getEffekt()).isEqualTo(nyBehandlingAktivitet.getBehandlingAktivitetData().getEffekt());
            s.assertAll();
        });
    }

    @Test
    public void skalMappeMoteData() {
        AktivitetData nyMoteAktivitet = AktivitetDataTestBuilder.nyMoteAktivitet();
        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(nyMoteAktivitet, false);
        SoftAssertions.assertSoftly( s -> {
            s.assertThat(aktivitetDTO.getType()).isEqualTo(AktivitetTypeDTO.MOTE);
            s.assertThat(aktivitetDTO.getAdresse()).isEqualTo(nyMoteAktivitet.getMoteData().getAdresse());
            s.assertThat(aktivitetDTO.getForberedelser()).isEqualTo(nyMoteAktivitet.getMoteData().getForberedelser());
            s.assertThat(aktivitetDTO.getKanal()).isEqualTo(nyMoteAktivitet.getMoteData().getKanal());
            s.assertThat(aktivitetDTO.getReferat()).isEqualTo(nyMoteAktivitet.getMoteData().getReferat());
            s.assertThat(aktivitetDTO.isErReferatPublisert()).isEqualTo(nyMoteAktivitet.getMoteData().isReferatPublisert());
            s.assertAll();
        });
    }

    @Test
    public void skalMappeStillingFraNavData() {
        AktivitetData nyStillingFraNav = AktivitetDataTestBuilder.nyStillingFraNav();
        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(nyStillingFraNav, false);
        SoftAssertions.assertSoftly( s -> {
            s.assertThat(aktivitetDTO.getType()).isEqualTo(AktivitetTypeDTO.STILLING_FRA_NAV);
            s.assertThat(aktivitetDTO.getStillingFraNavData().getKanDeles()).isEqualTo(nyStillingFraNav.getStillingFraNavData().getKanDeles());
            s.assertThat(aktivitetDTO.getStillingFraNavData().getCvKanDelesAv()).isEqualTo(nyStillingFraNav.getStillingFraNavData().getCvKanDelesAv());
            s.assertThat(aktivitetDTO.getStillingFraNavData().getCvKanDelesAvType()).isEqualTo(nyStillingFraNav.getStillingFraNavData().getCvKanDelesAvType());
            s.assertThat(aktivitetDTO.getStillingFraNavData().getCvKanDelesTidspunkt()).isEqualTo(nyStillingFraNav.getStillingFraNavData().getCvKanDelesTidspunkt());

            s.assertAll();
        });
    }

    @Test
    public void type_SAMTALEREFERAT_should_always_return_false() {
        AktivitetData data = AktivitetData
                .builder()
                .id(random.nextLong())
                .versjon(random.nextLong())
                .lagtInnAv(InnsenderData.NAV)
                .avtalt(true)
                .aktivitetType(AktivitetTypeData.SAMTALEREFERAT)
                .build();
        AktivitetDTO dto = AktivitetDTOMapper.mapTilAktivitetDTO(data, false);
        assertThat(dto.isAvtalt()).isFalse();
    }

    @Test
    public void type_except_SAMTALEREFERAT_should_return_actual_value() {
        AktivitetData data = AktivitetData
                .builder()
                .id(random.nextLong())
                .versjon(random.nextLong())
                .lagtInnAv(InnsenderData.NAV)
                .avtalt(true)
                .aktivitetType(getTypeExceptSAMTALEREFERAT())
                .build();
        AktivitetDTO dto = AktivitetDTOMapper.mapTilAktivitetDTO(data, false);
        assertThat(dto.isAvtalt()).isTrue();
    }

    private static AktivitetTypeData getTypeExceptSAMTALEREFERAT() {
        AktivitetTypeData type = null;
        while (type == null) {
            int i = random.nextInt(AktivitetTypeData.values().length);
            type = AktivitetTypeData.values()[i];
            if (type == AktivitetTypeData.SAMTALEREFERAT) {
                type = null;
            }
        }
        return type;
    }

}