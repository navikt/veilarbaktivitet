package no.nav.veilarbaktivitet.testutils;

import no.nav.veilarbaktivitet.domain.*;

import java.time.ZonedDateTime;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static no.nav.veilarbaktivitet.domain.AktivitetData.AktivitetDataBuilder;

public class AktivitetDataTestBuilder {

    public static AktivitetDataBuilder nyAktivitet() {
        ZonedDateTime fraDato = nyDato();

        return AktivitetData.builder()
                .id(new Random().nextLong())
                .aktorId("kake")
                .fraDato(fraDato)
                .tilDato(fraDato.plusDays(1))
                .tittel("tittel")
                .beskrivelse("beskrivelse")
                .status(AktivitetStatus.values()[0])
                .avsluttetKommentar("avsluttetKommentar")
                .lagtInnAv(InnsenderData.values()[0])
                .opprettetDato(fraDato.minusDays(1))
                .lenke("lenke")
                .transaksjonsType(AktivitetTransaksjonsType.OPPRETTET)
                .lestAvBrukerForsteGang(null)
                .historiskDato(null)
                .malid("2");
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

    public static ZonedDateTime nyDato() {
        return ZonedDateTime.now()
                .plusDays(ThreadLocalRandom.current().nextInt(30))
                .minusHours(ThreadLocalRandom.current().nextInt(24));
    }

}
