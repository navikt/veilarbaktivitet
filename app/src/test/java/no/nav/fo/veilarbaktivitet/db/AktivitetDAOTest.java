package no.nav.fo.veilarbaktivitet.db;

import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarbaktivitet.domain.*;
import org.junit.Test;

import javax.inject.Inject;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Calendar.SECOND;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.EGENAKTIVITET;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.JOBBSØKING;
import static org.apache.commons.lang3.time.DateUtils.truncate;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

public class AktivitetDAOTest extends IntegrasjonsTest {

    private static final String AKTOR_ID = "1234";

    @Inject
    private AktivitetDAO aktivitetDAO;

    @Test
    public void opprette_og_hente_egenaktivitet() {
        AktivitetData aktivitet = nyAktivitet(AKTOR_ID).setAktivitetType(EGENAKTIVITET);
        EgenAktivitetData egenAktivitet = new EgenAktivitetData().setAktivitet(aktivitet);

        aktivitetDAO.opprettEgenAktivitet(egenAktivitet);

        List<EgenAktivitetData> egenAktiviteter = aktivitetDAO.hentEgenAktiviteterForAktorId(AKTOR_ID);
        assertThat(egenAktiviteter, hasSize(1));
        assertThat(egenAktivitet, equalTo(egenAktiviteter.get(0)));
    }

    @Test
    public void opprette_og_hente_stillingaktivitet() {
        AktivitetData aktivitet = nyAktivitet(AKTOR_ID).setAktivitetType(JOBBSØKING);
        StillingsoekData stillingsøk = nyttStillingssøk();
        StillingsSoekAktivitet stillingsSøkAktivitet = new StillingsSoekAktivitet().setAktivitet(aktivitet).setStillingsoek(stillingsøk);

        aktivitetDAO.opprettStillingAktivitet(stillingsSøkAktivitet);

        List<StillingsSoekAktivitet> stillingsSøkAktiviteter = aktivitetDAO.hentStillingsAktiviteterForAktorId(AKTOR_ID);
        assertThat(stillingsSøkAktiviteter, hasSize(1));
        assertThat(stillingsSøkAktivitet, equalTo(stillingsSøkAktiviteter.get(0)));
    }

    private AktivitetData nyAktivitet(String aktorId) {
        return new AktivitetData()
                .setAktorId(aktorId)
                .setFraDato(nyDato())
                .setTilDato(nyDato())
                .setTittel("tittel")
                .setBeskrivelse("beskrivelse")
                .setStatus(AktivitetStatusData.values()[0])
                .setAvsluttetDato(nyDato())
                .setAvsluttetKommentar("avsluttetKommentar")
                .setLagtInnAv(InnsenderData.values()[0])
                .setOpprettetDato(nyDato())
                .setLenke("lenke")
                .setDeleMedNav(true)
                .setKommentarer(asList(nyKommentar(), nyKommentar()))
                ;
    }

    private KommentarData nyKommentar() {
        return new KommentarData()
                .setKommentar("kommentar")
                .setOpprettetAv("opprettetAv")
                .setOpprettetDato(nyDato())
                ;
    }

    private StillingsoekData nyttStillingssøk() {
        return new StillingsoekData()
                .setArbeidsgiver("arbeidsgiver")
                .setKontaktPerson("kontaktperson")
                .setStillingsTittel("stilingstittel")
                .setStillingsoekEtikett(StillingsoekEtikettData.values()[0]);
    }

    private Date nyDato() {
        return truncate(new Date(new Random().nextLong() % System.currentTimeMillis()), SECOND);
    }

}