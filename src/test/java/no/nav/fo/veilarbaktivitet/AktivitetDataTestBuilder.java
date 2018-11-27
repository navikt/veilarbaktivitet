package no.nav.fo.veilarbaktivitet;

import no.nav.fo.veilarbaktivitet.domain.*;

import java.util.Date;
import java.util.Random;

import static java.util.Calendar.SECOND;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetData.AktivitetDataBuilder;
import static org.apache.commons.lang3.time.DateUtils.truncate;

public class AktivitetDataTestBuilder {

    public static AktivitetDataBuilder nyAktivitet() {
        return AktivitetData.builder()
                .id(new Random().nextLong())
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

    public static StillingsoekAktivitetData nyttStillingssøk() {
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

    public static Date nyDato() {
        return truncate(new Date(new Random().nextLong() % System.currentTimeMillis()), SECOND);
    }


}
