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
                .id(new Random().nextLong())
                .aktorId("kake")
                .fraDato(nyDato())
                .tilDato(nyDato())
                .tittel("tittel")
                .beskrivelse("beskrivelse")
                .status(AktivitetStatus.values()[0])
                .avsluttetKommentar("avsluttetKommentar")
                .lagtInnAv(InnsenderData.values()[0])
                .opprettetDato(nyDato())
                .lenke("lenke")
                .transaksjonsType(AktivitetTransaksjonsType.DETALJER_ENDRET)
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


    public static Date nyDato() {
        return truncate(new Date(new Random().nextLong() % System.currentTimeMillis()), SECOND);
    }


}
