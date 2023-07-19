package no.nav.veilarbaktivitet.testutils;

import lombok.SneakyThrows;
import no.nav.veilarbaktivitet.aktivitet.domain.*;
import no.nav.veilarbaktivitet.aktivitet.dto.KanalDTO;
import no.nav.veilarbaktivitet.aktivitetskort.*;
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Attributt;
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Etikett;
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Oppgave;
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Oppgaver;
import no.nav.veilarbaktivitet.person.Innsender;
import no.nav.veilarbaktivitet.stilling_fra_nav.*;

import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class AktivitetTypeDataTestBuilder {

    public static StillingsoekAktivitetData nyttStillingssok() {
        return StillingsoekAktivitetData.builder()
                .arbeidsgiver("arbeidsgiver")
                .kontaktPerson("kontaktperson")
                .stillingsTittel("stilingstittel")
                .stillingsoekEtikett(StillingsoekEtikettData.values()[0])
                .build()
                ;
    }

    public static EgenAktivitetData nyEgenaktivitet() {
        return EgenAktivitetData.builder()
                .hensikt("nada")
                .oppfolging("oppfølging")
                .build();
    }

    public static MoteData moteData() {
        return MoteData.builder()
                .adresse("en adresse")
                .forberedelser("en forbedredelse")
                .kanal(KanalDTO.values()[0])
                .referatPublisert(true)
                .referat("et referat")
                .build();
    }

    public static SokeAvtaleAktivitetData nySokeAvtaleAktivitet() {
        return SokeAvtaleAktivitetData.builder()
                .antallStillingerSokes(10)
                .antallStillingerIUken(2)
                .avtaleOppfolging("Oppfølging")
                .build();
    }

    public static IJobbAktivitetData nyIJobbAktivitet() {
        return IJobbAktivitetData.builder()
                .jobbStatusType(JobbStatusTypeData.HELTID)
                .ansettelsesforhold("Vikar")
                .arbeidstid("7,5 timer")
                .build();
    }

    public static BehandlingAktivitetData nyBehandlingAktivitet() {
        return BehandlingAktivitetData.builder()
                .behandlingType("Medisinsk")
                .behandlingSted("Legen")
                .effekt("Bli frisk")
                .behandlingOppfolging("Husk å ta pillene dine")
                .build();
    }


    public static StillingFraNavData nyStillingFraNav(boolean setCVKanDelesData) {
        CvKanDelesData cvKanDelesData = null;

        if(setCVKanDelesData){
            cvKanDelesData = CvKanDelesData.builder()
                    .endretAvType(Innsender.NAV)
                    .endretAv("Z999999")
                    .endretTidspunkt(AktivitetDataTestBuilder.nyDato())
                    .kanDeles(Boolean.TRUE)
                    .build();
        }

        KontaktpersonData kontaktpersonData = KontaktpersonData.builder()
                .navn("Ola Nordmann")
                .tittel("NAV-ansatt")
                .mobil("10203040")
                .build();

        return StillingFraNavData.builder()
                .bestillingsId("123")
                .stillingsId("1234")
                .cvKanDelesData(cvKanDelesData)
                .arbeidsgiver("NAV IT")
                .arbeidssted("Oslo")
                .soknadsfrist(new Date().toString())
                .svarfrist(new Date(Instant.now().plus(2, ChronoUnit.DAYS).toEpochMilli()))
                .kontaktpersonData(kontaktpersonData)
                .soknadsstatus(Soknadsstatus.VENTER)
                .livslopsStatus(LivslopsStatus.KAN_IKKE_VARSLE)
                .build();
    }

    @SneakyThrows
    public static EksternAktivitetData eksternAktivitetData() {
        return EksternAktivitetData.builder()
                .type(AktivitetskortType.ARENA_TILTAK)
                .source("AKTIVITET_ARENA_ACL")
                .tiltaksKode("ABIST")
                .oppgave(new Oppgaver(new Oppgave("tekst", "subtekst", new URL("https://www.nav.no")), null))
                .detalj(new Attributt("Arrangør", "NAV"))
                .detalj(new Attributt("Dager per uke", "5"))
                .etikett(new Etikett("GJENN"))
                .build();
    }
}
