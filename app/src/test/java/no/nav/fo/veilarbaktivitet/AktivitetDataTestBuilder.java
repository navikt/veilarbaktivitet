package no.nav.fo.veilarbaktivitet;

import no.nav.fo.veilarbaktivitet.domain.*;

import java.util.Date;
import java.util.Random;

import static java.util.Calendar.SECOND;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetData.AktivitetDataBuilder;
import static org.apache.commons.lang3.time.DateUtils.truncate;

public class AktivitetDataTestBuilder {

    public static AktivitetDataBuilder nyAktivitet(String aktorId) {
        return AktivitetData.builder()
                .id(new Random().nextLong())
                .aktorId(aktorId)
                .fraDato(nyDato())
                .tilDato(nyDato())
                .tittel("tittel")
                .beskrivelse("beskrivelse")
                .status(AktivitetStatus.values()[0])
                .avsluttetKommentar("avsluttetKommentar")
                .lagtInnAv(InnsenderData.values()[0])
                .opprettetDato(nyDato())
                .lenke("lenke")
                .transaksjonsTypeData(TransaksjonsTypeData.DETALJER_ENDRET);
    }

    public static StillingsoekAktivitetData nyttStillingssøk() {
        return new StillingsoekAktivitetData()
                .setArbeidsgiver("arbeidsgiver")
                .setKontaktPerson("kontaktperson")
                .setStillingsTittel("stilingstittel")
                .setStillingsoekEtikett(StillingsoekEtikettData.values()[0]);
    }

    public static Date nyDato() {
        return truncate(new Date(new Random().nextLong() % System.currentTimeMillis()), SECOND);
    }


}
