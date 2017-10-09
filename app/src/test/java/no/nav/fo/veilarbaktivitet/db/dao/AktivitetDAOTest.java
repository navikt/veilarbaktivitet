package no.nav.fo.veilarbaktivitet.db.dao;

import lombok.val;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.domain.AktivitetTransaksjonsType;
import org.junit.Test;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import static no.nav.fo.veilarbaktivitet.AktivitetDataTestBuilder.*;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

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
    public void opprette_og_hente_ijobbaktivitet() {
        val aktivitet = gitt_at_det_finnes_en_ijobb();
        val aktiviteter = aktivitetDAO.hentAktiviteterForAktorId(AKTOR_ID);

        assertThat(aktiviteter, hasSize(1));
        assertThat(aktivitet, equalTo(aktiviteter.get(0)));
    }

    @Test
    public void opprette_og_hente_behandlingaktiviete() {
        val aktivitet = gitt_at_det_finnes_en_behandling();
        val aktiviteter = aktivitetDAO.hentAktiviteterForAktorId(AKTOR_ID);

        assertThat(aktiviteter, hasSize(1));
        assertThat(aktivitet, equalTo(aktiviteter.get(0)));
    }

    @Test
    public void opprette_og_hente_mote() {
        val aktivitet = gitt_at_det_finnes_et_mote();
        val aktiviteter = aktivitetDAO.hentAktiviteterForAktorId(AKTOR_ID);

        assertThat(aktiviteter, hasSize(1));
        assertThat(aktivitet, equalTo(aktiviteter.get(0)));
    }

    @Test
    public void opprette_og_hente_samtalereferat() {
        val aktivitet = gitt_at_det_finnes_et_samtalereferat();
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

    @Test
    public void hent_aktivitet() {
        val aktivitet = gitt_at_det_finnes_en_stillings_aktivitet();

        val hentetAktivitet = aktivitetDAO.hentAktivitet(aktivitet.getId());
        assertThat(aktivitet, equalTo(hentetAktivitet));
    }

    @Test
    public void transaksjonsTypene_er_rett_satt_opp() {
        val aktivitetBuilder = nyAktivitet()
                .aktivitetType(EGENAKTIVITET)
                .egenAktivitetData(nyEgenaktivitet());

        Arrays.asList(AktivitetTransaksjonsType.values()).forEach(t -> {
                    aktivitetBuilder.transaksjonsType(t);
                    try {
                        insertAktivitet(aktivitetBuilder.build());
                    } catch (Exception e) {
                        fail("TransaksjonsTypen er ikke lagt inn i databasen");
                    }
                }
        );
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

    private AktivitetData gitt_at_det_finnes_en_ijobb() {
        return insertAktivitet(nyAktivitet()
                .aktivitetType(IJOBB)
                .iJobbAktivitetData(nyIJobbAktivitet())
                .build()
        );
    }

    private AktivitetData gitt_at_det_finnes_en_behandling() {
        return insertAktivitet(nyAktivitet()
                .aktivitetType(BEHANDLING)
                .behandlingAktivitetData(nyBehandlingAktivitet())
                .build()
        );
    }

    private AktivitetData gitt_at_det_finnes_et_mote() {
        return insertAktivitet(nyAktivitet()
                .aktivitetType(MOTE)
                .moteData(moteData())
                .build()
        );
    }

    private AktivitetData gitt_at_det_finnes_et_samtalereferat() {
        return insertAktivitet(nyAktivitet()
                .aktivitetType(SAMTALEREFERAT)
                .moteData(moteData())
                .build()
        );
    }

    private AktivitetData insertAktivitet(AktivitetData aktivitet) {
        val id = Optional.ofNullable(aktivitet.getId()).orElseGet(aktivitetDAO::getNextUniqueAktivitetId);
        val aktivitetMedId = aktivitet.toBuilder()
                .id(id)
                .aktorId(AKTOR_ID)
                .build();

        val endret = new Date();
        aktivitetDAO.insertAktivitet(aktivitetMedId, endret);
        return aktivitetDAO.hentAktivitet(id);
    }
}