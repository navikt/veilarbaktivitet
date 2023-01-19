package no.nav.veilarbaktivitet.testutils;

import no.nav.veilarbaktivitet.aktivitet.domain.*;
import no.nav.veilarbaktivitet.avtalt_med_nav.Forhaandsorientering;
import no.nav.veilarbaktivitet.avtalt_med_nav.Type;
import no.nav.veilarbaktivitet.person.Innsender;

import java.util.Date;
import java.util.Random;
import java.util.UUID;

import static java.util.Calendar.SECOND;
import static no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData.AktivitetDataBuilder;
import static org.apache.commons.lang3.time.DateUtils.truncate;

public class AktivitetDataTestBuilder {

    public static AktivitetDataBuilder nyAktivitet() {
        return AktivitetData.builder()
                .id(new Random().nextLong()) // Hvis denne persisteres, vil den få en ny id fra sekvens
                .aktorId("kake")
                .versjon(1L) // Hvis denne persisteres vil den få en ny versjon fra sekvens
                .fraDato(nyDato())
                .tilDato(nyDato())
                .tittel("tittel")
                .beskrivelse("beskrivelse")
                .versjon(new Random().nextLong())
                .status(AktivitetStatus.PLANLAGT)
                .avsluttetKommentar("avsluttetKommentar")
                .endretAvType(Innsender.values()[0])
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
                return nyttStillingssok();
            case EGENAKTIVITET:
                return nyEgenaktivitet();
            case SAMTALEREFERAT:
                return nySamtaleReferat();
            case STILLING_FRA_NAV:
                return nyStillingFraNavMedCVKanDeles();
            case EKSTERNAKTIVITET:
                return nyEksternAktivitet();
            default: throw new IllegalArgumentException("ukjent type");
        }
    }

    public static AktivitetData nyStillingFraNavMedCVKanDeles() {
        return AktivitetDataTestBuilder.nyAktivitet()
                .aktivitetType(AktivitetTypeData.STILLING_FRA_NAV)
                .stillingFraNavData(AktivitetTypeDataTestBuilder.nyStillingFraNav(true))
                .build();
    }

    public static AktivitetData nyStillingFraNav() {
        return AktivitetDataTestBuilder.nyAktivitet()
                .aktivitetType(AktivitetTypeData.STILLING_FRA_NAV)
                .stillingFraNavData(AktivitetTypeDataTestBuilder.nyStillingFraNav(false))
                .build();
    }

    public static AktivitetData nyttStillingssok() {

        return AktivitetDataTestBuilder.nyAktivitet()
                .aktivitetType(AktivitetTypeData.JOBBSOEKING)
                .stillingsSoekAktivitetData(AktivitetTypeDataTestBuilder.nyttStillingssok())
                .build();
    }

    public static AktivitetData nyEgenaktivitet() {

        return AktivitetDataTestBuilder.nyAktivitet()
                .aktivitetType(AktivitetTypeData.EGENAKTIVITET)
                .egenAktivitetData(AktivitetTypeDataTestBuilder.nyEgenaktivitet())
                .build();
    }

    public static AktivitetData nySokeAvtaleAktivitet() {

        return AktivitetDataTestBuilder.nyAktivitet()
                .aktivitetType(AktivitetTypeData.SOKEAVTALE)
                .sokeAvtaleAktivitetData(AktivitetTypeDataTestBuilder.nySokeAvtaleAktivitet())
                .build();
    }

    public static AktivitetData nyIJobbAktivitet() {
        return AktivitetDataTestBuilder.nyAktivitet()
                .aktivitetType(AktivitetTypeData.IJOBB)
                .iJobbAktivitetData(AktivitetTypeDataTestBuilder.nyIJobbAktivitet())
                .build();
    }

    public static AktivitetData nyBehandlingAktivitet() {
        return AktivitetDataTestBuilder.nyAktivitet()
                .aktivitetType(AktivitetTypeData.BEHANDLING)
                .behandlingAktivitetData(AktivitetTypeDataTestBuilder.nyBehandlingAktivitet())
                .build();
    }

    public static AktivitetData nyMoteAktivitet() {
        return AktivitetDataTestBuilder.nyAktivitet()
                .aktivitetType(AktivitetTypeData.MOTE)
                .moteData(AktivitetTypeDataTestBuilder.moteData())
                .build();
    }

    public static AktivitetData nySamtaleReferat() {
        return AktivitetDataTestBuilder.nyAktivitet()
                .aktivitetType(AktivitetTypeData.SAMTALEREFERAT)
                .moteData(AktivitetTypeDataTestBuilder.moteData())
                .tilDato(null)
                .build();
    }


    public static AktivitetData nyEksternAktivitet() {
        return AktivitetDataTestBuilder.nyAktivitet()
                .aktivitetType(AktivitetTypeData.EKSTERNAKTIVITET)
                .funksjonellId(UUID.randomUUID())
                .eksternAktivitetData(AktivitetTypeDataTestBuilder.eksternAktivitetData())
                .build();
    }

    public static Forhaandsorientering nyForhaandorientering() {
        return Forhaandsorientering.builder()
                .id(UUID.randomUUID().toString())
                .tekst("Lol")
                .aktivitetId("abc")
                .type(Type.SEND_PARAGRAF_11_9)
                .opprettetAv("meg")
                .opprettetDato(new Date())
                .build();
    }


    public static Date nyDato() {
        return truncate(new Date(new Random().nextLong() % System.currentTimeMillis()), SECOND);
    }


}
