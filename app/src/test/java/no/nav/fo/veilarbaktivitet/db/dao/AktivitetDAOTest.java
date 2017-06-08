package no.nav.fo.veilarbaktivitet.db.dao;

import lombok.val;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import org.junit.Test;
import org.springframework.dao.DuplicateKeyException;

import javax.inject.Inject;

import java.util.Optional;

import static no.nav.fo.veilarbaktivitet.AktivitetDataTestBuilder.*;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

public class AktivitetDAOTest extends IntegrasjonsTest {

    private static final String AKTOR_ID = "1234";
    private long versjon = 1;

    @Inject
    private AktivitetDAO aktivitetDAO;

    @Test
    public void opprette_og_hente_egenaktivitet() {
        val aktivitet = gitt_at_det_finnes_en_egen_aktivitet();

        val aktiviteter = aktivitetDAO.hentAktiviteterForAktorId(AKTOR_ID);
        assertThat(aktiviteter, hasSize(1));
        assertThat(aktivitet, equalTo(aktiviteter.get(0)));
    }

    @Test
    public void opprette_og_hente_stillingaktivitet() {
        val aktivitet = gitt_at_det_finnes_en_stillings_aktivitet();

        val aktiviteter = aktivitetDAO.hentAktiviteterForAktorId(AKTOR_ID);
        assertThat(aktiviteter, hasSize(1));
        assertThat(aktivitet, equalTo(aktiviteter.get(0)));
    }

    @Test
    public void opprette_og_hente_sokeavtaleaktivitet() {
        val aktivitet = gitt_at_det_finnes_en_sokeavtale();

        val aktiviteter = aktivitetDAO.hentAktiviteterForAktorId(AKTOR_ID);
        assertThat(aktiviteter, hasSize(1));
        assertThat(aktivitet, equalTo(aktiviteter.get(0)));
    }

    @Test
    public void slett_aktivitet() {
        val aktivitet = gitt_at_det_finnes_en_stillings_aktivitet();

        aktivitetDAO.slettAktivitet(aktivitet.getId());

        assertThat(aktivitetDAO.hentAktiviteterForAktorId(AKTOR_ID), empty());
    }

    @Test(expected = DuplicateKeyException.class)
    public void versjonskonflikt() {
        lagaktivitetMedIdVersjon(10L, 22L);
        lagaktivitetMedIdVersjon(10L, 22L);
    }

    @Test
    public void hent_aktivitet() {
        val aktivitet = gitt_at_det_finnes_en_stillings_aktivitet();

        val hentetAktivitet = aktivitetDAO.hentAktivitet(aktivitet.getId());
        assertThat(aktivitet, equalTo(hentetAktivitet));
    }

    private AktivitetData gitt_at_det_finnes_en_stillings_aktivitet() {
        val aktivitet = nyAktivitet()
                .aktivitetType(JOBBSOEKING)
                .stillingsSoekAktivitetData(nyttStillingssøk())
                .build();

        return insertAktivitet(aktivitet);
    }

    private AktivitetData gitt_at_det_finnes_en_egen_aktivitet() {
        val aktivitet = nyAktivitet()
                .aktivitetType(EGENAKTIVITET)
                .egenAktivitetData(nyEgenaktivitet())
                .build();

        return insertAktivitet(aktivitet);
    }

    private AktivitetData gitt_at_det_finnes_en_sokeavtale() {
        val aktivitet = nyAktivitet()
                .aktivitetType(SOKEAVTALE)
                .sokeAvtaleAktivitetData(nySokeAvtaleAktivitet())
                .build();

        return insertAktivitet(aktivitet);
    }

    private AktivitetData lagaktivitetMedIdVersjon(Long id, Long versjon) {
        val aktivitet = nyAktivitet()
                .id(id)
                .versjon(versjon)
                .aktivitetType(SOKEAVTALE)
                .sokeAvtaleAktivitetData(nySokeAvtaleAktivitet())
                .build();

        return insertAktivitet(aktivitet);
    }

    private AktivitetData insertAktivitet(AktivitetData aktivitet) {
        val id = Optional.ofNullable(aktivitet.getId()).orElseGet(aktivitetDAO::getNextUniqueAktivitetId);
        val nyVersjon = Optional.ofNullable(aktivitet.getVersjon()).orElseGet(() -> versjon++);
        val aktivitetMedId = aktivitet.toBuilder()
                .id(id)
                .versjon(nyVersjon)
                .aktorId(AKTOR_ID)
                .build();

        aktivitetDAO.insertAktivitet(aktivitetMedId);
        return aktivitetMedId.toBuilder().id(id).versjon(versjon).build();
    }
}