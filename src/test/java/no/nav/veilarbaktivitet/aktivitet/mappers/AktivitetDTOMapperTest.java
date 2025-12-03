package no.nav.veilarbaktivitet.aktivitet.mappers;

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.aktivitet.domain.MoteData;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.avtalt_med_nav.Forhaandsorientering;
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDTO;
import no.nav.veilarbaktivitet.avtalt_med_nav.Type;
import no.nav.veilarbaktivitet.stilling_fra_nav.Soknadsstatus;
import no.nav.veilarbaktivitet.stilling_fra_nav.StillingFraNavData;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;


class AktivitetDTOMapperTest {
    @Test
    void skalMappeAktivitetsFelter() {
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
                    s.assertThat(aktivitetDTO.getEndretAvType()).isEqualTo(aktivitetData.getEndretAvType().name());
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
    void skalMappeForhaandsorientering() {
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
    void skalMappeStillingSokData() {
        AktivitetData nyttStillingssok = AktivitetDataTestBuilder.nyttStillingssok();
        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(nyttStillingssok, false);
        SoftAssertions.assertSoftly( s -> {
            s.assertThat(aktivitetDTO.getType()).isEqualTo(AktivitetTypeDTO.STILLING);
            s.assertThat(aktivitetDTO.getArbeidsgiver()).isEqualTo(nyttStillingssok.getStillingsSoekAktivitetData().getArbeidsgiver());
            s.assertThat(aktivitetDTO.getStillingsTittel()).isEqualTo(nyttStillingssok.getStillingsSoekAktivitetData().getStillingsTittel());
            s.assertThat(aktivitetDTO.getArbeidssted()).isEqualTo(nyttStillingssok.getStillingsSoekAktivitetData().getArbeidssted());
            s.assertThat(aktivitetDTO.getKontaktperson()).isEqualTo(nyttStillingssok.getStillingsSoekAktivitetData().getKontaktPerson());
            s.assertThat(aktivitetDTO.getEtikett()).isEqualTo(Helpers.Etikett.getDTO(nyttStillingssok.getStillingsSoekAktivitetData().getStillingsoekEtikett()));

            s.assertAll();
        });
    }

    @Test
    void skalMappeEgenAktivitetData() {
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
    void skalMappeSokeAvtaleData() {
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
    void skalMappeIJobbData() {
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
    void skalMappeBehandleAktivitetData() {
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
    void skalMappeMoteData() {
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
    void skalIkkeViseReferatNaarEksternOgIkkePublisert() {
        AktivitetData moteAktivitet = AktivitetDataTestBuilder.nyMoteAktivitet();
        MoteData moteData = moteAktivitet.getMoteData().withReferat("Referat").withReferatPublisert(false);
        AktivitetData nyMoteAktivitet = moteAktivitet.withMoteData(moteData);

        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(nyMoteAktivitet, true);
        SoftAssertions.assertSoftly( s -> {
            s.assertThat(aktivitetDTO.getReferat()).isNull();
            s.assertThat(aktivitetDTO.isErReferatPublisert()).isEqualTo(false);
            s.assertAll();
        });
    }

    @Test
    void skalViseReferatNaarInternOgIkkePublisert() {
        AktivitetData moteAktivitet = AktivitetDataTestBuilder.nyMoteAktivitet();
        MoteData moteData = moteAktivitet.getMoteData().withReferat("Referat").withReferatPublisert(false);
        AktivitetData nyMoteAktivitet = moteAktivitet.withMoteData(moteData);

        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(nyMoteAktivitet, false);
        SoftAssertions.assertSoftly( s -> {
            s.assertThat(aktivitetDTO.getReferat()).isEqualTo("Referat");
            s.assertThat(aktivitetDTO.isErReferatPublisert()).isEqualTo(false);
            s.assertAll();
        });
    }

    @Test
    void skalViseReferatNaarReferatErPublisert() {
        AktivitetData moteAktivitet = AktivitetDataTestBuilder.nyMoteAktivitet();
        MoteData moteData = moteAktivitet.getMoteData().withReferat("Referat").withReferatPublisert(true);
        AktivitetData nyMoteAktivitet = moteAktivitet.withMoteData(moteData);

        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(nyMoteAktivitet, true);
        SoftAssertions.assertSoftly( s -> {
            s.assertThat(aktivitetDTO.getReferat()).isEqualTo("Referat");
            s.assertThat(aktivitetDTO.isErReferatPublisert()).isEqualTo(true);
            s.assertAll();
        });

        AktivitetDTO aktivitetDTO2 = AktivitetDTOMapper.mapTilAktivitetDTO(nyMoteAktivitet, false);
        SoftAssertions.assertSoftly( s -> {
            s.assertThat(aktivitetDTO2.getReferat()).isEqualTo("Referat");
            s.assertThat(aktivitetDTO2.isErReferatPublisert()).isEqualTo(true);
            s.assertAll();
        });
    }

    @Test
    void skalMappeStillingFraNavData() {
        AktivitetData nyStillingFraNav = AktivitetDataTestBuilder.nyStillingFraNavMedCVKanDeles();
        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(nyStillingFraNav, false);

        SoftAssertions.assertSoftly( s -> {
            s.assertThat(aktivitetDTO.getType()).isEqualTo(AktivitetTypeDTO.STILLING_FRA_NAV);
            s.assertThat(aktivitetDTO.getStillingFraNavData().getCvKanDelesData().getKanDeles()).isEqualTo(nyStillingFraNav.getStillingFraNavData().getCvKanDelesData().getKanDeles());
            s.assertThat(aktivitetDTO.getStillingFraNavData().getCvKanDelesData().getEndretAv()).isEqualTo(nyStillingFraNav.getStillingFraNavData().getCvKanDelesData().getEndretAv());
            s.assertThat(aktivitetDTO.getStillingFraNavData().getCvKanDelesData().getEndretAvType()).isEqualTo(nyStillingFraNav.getStillingFraNavData().getCvKanDelesData().getEndretAvType());
            s.assertThat(aktivitetDTO.getStillingFraNavData().getCvKanDelesData().getEndretTidspunkt()).isEqualTo(nyStillingFraNav.getStillingFraNavData().getCvKanDelesData().getEndretTidspunkt());

            s.assertAll();
        });
    }

}
