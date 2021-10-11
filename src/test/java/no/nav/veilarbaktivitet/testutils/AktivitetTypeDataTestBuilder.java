package no.nav.veilarbaktivitet.testutils;

import no.nav.veilarbaktivitet.aktivitet.domain.*;
import no.nav.veilarbaktivitet.aktivitet.dto.KanalDTO;
import no.nav.veilarbaktivitet.person.InnsenderData;
import no.nav.veilarbaktivitet.stilling_fra_nav.*;

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
                .antallStillingerSokes(10L)
                .antallStillingerIUken(2L)
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

    public static MoteData nyMote() {
        return MoteData.builder()
                .adresse("123")
                .kanal(KanalDTO.INTERNETT)
                .forberedelser("blee")
                .referat("temp")
                .referatPublisert(false)
                .build();
    }

    public static StillingFraNavData nyStillingFraNav(boolean setCVKanDelesData) {
        CvKanDelesData cvKanDelesData = null;

        if(setCVKanDelesData){
            cvKanDelesData = CvKanDelesData.builder()
                    .endretAvType(InnsenderData.NAV)
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
                .kontaktpersonData(kontaktpersonData)
                .soknadsstatus(Soknadsstatus.VENTER)
                .livslopsStatus(LivslopsStatus.KAN_IKKE_VARSLE)
                .build();
    }
}
