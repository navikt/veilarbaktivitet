package no.nav.fo.veilarbaktivitet.db;

import lombok.val;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarbaktivitet.domain.*;
import org.junit.Test;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

import static org.apache.commons.lang3.time.DateUtils.truncate;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class EndringsloggDAOTest extends IntegrasjonsTest {

    private static final String endretAv = "BATMAN!!";
    private static final String endringsBeskrivelse = "one does not simply change anything";

    @Inject
    private EndringsLoggDAO endringsLoggDao;

    @Inject
    private AktivitetDAO aktivitetDAO;

    @Test
    public void opprett_og_hent_endringslogg() {
        val aktivitetId = opprett_egenaktivitet();
        endringsLoggDao.opprettEndringsLogg(aktivitetId, endretAv, endringsBeskrivelse);

        val endringsLoggs = endringsLoggDao.hentEndringdsloggForAktivitetId(aktivitetId);

        assertThat(endringsLoggs, hasSize(1));
        assertThat(endretAv, equalTo(endringsLoggs.get(0).getEndretAv()));
        assertThat(endringsBeskrivelse, equalTo(endringsLoggs.get(0).getEndringsBeskrivelse()));
        assertTrue("Dates are close enough",
                (new Date().getTime() - endringsLoggs.get(0).getEndretDato().getTime()) < 1000);
    }

    private long opprett_egenaktivitet() {
        val aktorId = "123";
        AktivitetData aktivitet = nyAktivitet(aktorId);
        EgenAktivitetData egenAktivitet = new EgenAktivitetData().setAktivitet(aktivitet);

        aktivitetDAO.opprettEgenAktivitet(egenAktivitet);

        List<EgenAktivitetData> egenAktiviteter = aktivitetDAO.hentEgenAktiviteterForAktorId(aktorId);
        return egenAktiviteter.get(0).getAktivitet().getId();
    }

    private AktivitetData nyAktivitet(String aktorId) {
        return new AktivitetData()
                .setAktorId(aktorId)
                .setTittel("tittel")
                .setBeskrivelse("beskrivelse")
                .setStatus(AktivitetStatusData.values()[0])
                .setAvsluttetKommentar("avsluttetKommentar")
                .setLagtInnAv(InnsenderData.values()[0])
                .setLenke("lenke")
                .setDeleMedNav(true)
                ;
    }

}