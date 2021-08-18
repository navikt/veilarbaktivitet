package no.nav.veilarbaktivitet.testutils;

import no.nav.veilarbaktivitet.domain.*;

import java.util.Date;
import java.util.Random;

import static java.util.Calendar.SECOND;
import static no.nav.veilarbaktivitet.domain.AktivitetData.AktivitetDataBuilder;
import static org.apache.commons.lang3.time.DateUtils.truncate;

public class AktivitetDataTestBuilder {

    public static AktivitetDataBuilder nyAktivitet() {
        return AktivitetData.builder()
                .id(new Random().nextLong()) // Hvis denne persisteres, vil den få en ny id fra sekvens
                .aktorId("kake")
                .versjon(1l) // Hvis denne persisteres vil den få en ny versjon fra sekvens
                .fraDato(nyDato())
                .tilDato(nyDato())
                .tittel("tittel")
                .beskrivelse("beskrivelse")
                .versjon(new Random().nextLong())
                .status(AktivitetStatus.PLANLAGT)
                .avsluttetKommentar("avsluttetKommentar")
                .lagtInnAv(InnsenderData.values()[0])
                .opprettetDato(nyDato())
                .lenke("lenke")
                .transaksjonsType(AktivitetTransaksjonsType.DETALJER_ENDRET)
                .lestAvBrukerForsteGang(null)
                .historiskDato(null)
                .endretDato(nyDato())
                .endretAv("Z999999")
                .malid("2");
    }

    public static AktivitetData nyAktivitet(AktivitetTypeData aktivitetTypeData) {
        switch (aktivitetTypeData) {
            case MOTE:
                return nyMoteAktivitet();
            case IJOBB:
                return nyIJobbAktivitet();
            case BEHANDLING:
                return nyBehandlingAktivitet();
            case SOKEAVTALE:
                return nySokeAvtaleAktivitet();
            case JOBBSOEKING:
                return nyttStillingssøk();
            case EGENAKTIVITET:
                return nyEgenaktivitet();
            case SAMTALEREFERAT:
                return nytSamtaleReferat();
            case STILLING_FRA_NAV:
                return nyStillingFraNavMedCVKanDeles();
            default: throw new IllegalArgumentException("ukjent type");
        }

    }

    public static AktivitetData nyStillingFraNavMedCVKanDeles() {
        return AktivitetDataTestBuilder.nyAktivitet()
                .aktivitetType(AktivitetTypeData.STILLING_FRA_NAV)
                .stillingFraNavData(AktivitetTypeDataTesBuilder.nyStillingFraNav(true))
                .build();
    }

    public static AktivitetData nyStillingFraNav() {
        return AktivitetDataTestBuilder.nyAktivitet()
                .aktivitetType(AktivitetTypeData.STILLING_FRA_NAV)
                .stillingFraNavData(AktivitetTypeDataTesBuilder.nyStillingFraNav(false))
                .build();
    }

    public static AktivitetData nyttStillingssøk() {

        return AktivitetDataTestBuilder.nyAktivitet()
                .aktivitetType(AktivitetTypeData.JOBBSOEKING)
                .stillingsSoekAktivitetData(AktivitetTypeDataTesBuilder.nyttStillingssøk())
                .build();
    }

    public static AktivitetData nyEgenaktivitet() {

        return AktivitetDataTestBuilder.nyAktivitet()
                .aktivitetType(AktivitetTypeData.EGENAKTIVITET)
                .egenAktivitetData(AktivitetTypeDataTesBuilder.nyEgenaktivitet())
                .build();
    }

    public static AktivitetData nySokeAvtaleAktivitet() {

        return AktivitetDataTestBuilder.nyAktivitet()
                .aktivitetType(AktivitetTypeData.SOKEAVTALE)
                .sokeAvtaleAktivitetData(AktivitetTypeDataTesBuilder.nySokeAvtaleAktivitet())
                .build();
    }

    public static AktivitetData nyIJobbAktivitet() {
        return AktivitetDataTestBuilder.nyAktivitet()
                .aktivitetType(AktivitetTypeData.IJOBB)
                .iJobbAktivitetData(AktivitetTypeDataTesBuilder.nyIJobbAktivitet())
                .build();
    }

    public static AktivitetData nyBehandlingAktivitet() {
        return AktivitetDataTestBuilder.nyAktivitet()
                .aktivitetType(AktivitetTypeData.BEHANDLING)
                .behandlingAktivitetData(AktivitetTypeDataTesBuilder.nyBehandlingAktivitet())
                .build();
    }

    public static AktivitetData nyMoteAktivitet() {
        return AktivitetDataTestBuilder.nyAktivitet()
                .aktivitetType(AktivitetTypeData.MOTE)
                .moteData(AktivitetTypeDataTesBuilder.moteData())
                .build();
    }

    public static AktivitetData nytSamtaleReferat() {
        return AktivitetDataTestBuilder.nyAktivitet()
                .aktivitetType(AktivitetTypeData.SAMTALEREFERAT)
                .moteData(AktivitetTypeDataTesBuilder.moteData())
                .build();
    }


    public static Date nyDato() {
        return truncate(new Date(new Random().nextLong() % System.currentTimeMillis()), SECOND);
    }


}
