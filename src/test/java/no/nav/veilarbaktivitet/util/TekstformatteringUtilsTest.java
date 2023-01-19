package no.nav.veilarbaktivitet.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static no.nav.veilarbaktivitet.util.TekstformatteringUtils.storeForbokstaverStedsnavn;

public class TekstformatteringUtilsTest {
    // NORDRE LAND skal bli Nordre Land
    @Test
    void storForbokstav_nordre_land() {
        Assertions.assertThat(
                storeForbokstaverStedsnavn("NORDRE LAND")
        ).isEqualTo("Nordre Land");
    }

    // AUST-AGDER skal bli Aust-Agder
    @Test
    void storForbokstav_aust_bindestrek_agder() {
        Assertions.assertThat(
                storeForbokstaverStedsnavn("AUST-AGDER")
        ).isEqualTo("Aust-Agder");
    }

    // BØ (TELEMARK) skal bli Bø (Telemark)
    @Test
    void storForbokstav_parentes() {
        Assertions.assertThat(
                storeForbokstaverStedsnavn("BØ (TELEMARK)")
        ).isEqualTo("Bø (Telemark)");
    }

    // MO I RANA skal bli Mo i Rana
    @Test
    void storForbokstav_i_skal_fortsatt_vere_liten() {
        Assertions.assertThat(
                storeForbokstaverStedsnavn("MO I RANA")
        ).isEqualTo("Mo i Rana");
    }

    // MØRE OG ROMSDAL skal bli Møre og Romsdal
    @Test
    void storForbokstav_og_skal_fortsatt_vere_liten() {
        Assertions.assertThat(
                storeForbokstaverStedsnavn("MØRE OG ROMSDAL")
        ).isEqualTo("Møre og Romsdal");
    }
}
