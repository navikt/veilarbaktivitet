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
                .historiskDato(nyDato());
    }

    public static StillingsoekAktivitetData nyttStillingssøk() {
        return new StillingsoekAktivitetData()
                .setArbeidsgiver("arbeidsgiver")
                .setKontaktPerson("kontaktperson")
                .setStillingsTittel("stilingstittel")
                .setStillingsoekEtikett(StillingsoekEtikettData.values()[0]);
    }

    public static EgenAktivitetData nyEgenaktivitet() {
        return new EgenAktivitetData().setHensikt("nada");
    }

    public static SokeAvtaleAktivitetData nySokeAvtaleAktivitet() {
        return new SokeAvtaleAktivitetData()
                .setAntall(10L)
                .setAvtaleOppfolging("Oppfølging");
    }

    public static IJobbAktivitetData nyIJobbAktivitet() {
        return new IJobbAktivitetData()
                .setJobbStatusType(JobbStatusTypeData.HELTID)
                .setAnsettelsesforhold("Vikar")
                .setArbeidstid("7,5 timer");
    }

    public static BehandlingAktivitetData nyBehandlingAktivitet() {
        return new BehandlingAktivitetData()
                .setBehandlingType("Medisinsk")
                .setBehandlingSted("Legen")
                .setEffekt("Bli frisk")
                .setBehandlingOppfolging("Husk å ta pillene dine");
    }

    public static Date nyDato() {
        return truncate(new Date(new Random().nextLong() % System.currentTimeMillis()), SECOND);
    }


}
