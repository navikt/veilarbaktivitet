package no.nav.veilarbaktivitet.testutils;

import lombok.SneakyThrows;
import no.nav.veilarbaktivitet.aktivitet.domain.*;
import no.nav.veilarbaktivitet.aktivitet.dto.KanalDTO;
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType;
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.*;
import no.nav.veilarbaktivitet.person.Innsender;
import no.nav.veilarbaktivitet.stilling_fra_nav.*;

import java.net.URL;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

public class AktivitetTypeDataTestBuilder {

    public static StillingsoekAktivitetData nyttStillingssok() {
        return StillingsoekAktivitetData.builder()
                .arbeidsgiver("arbeidsgiver")
                .arbeidssted("arbeidssted")
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
    public static EksternAktivitetData eksternAktivitetData(AktivitetskortType aktivitetskortType) {
        return new EksternAktivitetData(
            "AKTIVITET_ARENA_ACL",
            "ABIST",
            false,
            null,
            null,
                aktivitetskortType,
            new Oppgaver(new Oppgave("tekst", "subtekst", new URL("https://www.nav.no")), null),
            List.of(),
            List.of(
               new Attributt("Arrangør", "NAV"),
               new Attributt("Dager per uke", "5")
           ),
           List.of(new Etikett("Gjennomfører", Sentiment.POSITIVE, "GJENN")),
           ZonedDateTime.now()
        );
    }
}
