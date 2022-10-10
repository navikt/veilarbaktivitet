package no.nav.veilarbaktivitet.stilling_fra_nav;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import static no.nav.veilarbaktivitet.stilling_fra_nav.DelingAvCvUtils.storForbokstavStedsnavn;

public class DelingAvCvUtilsTest {
    // NORDRE LAND skal bli Nordre Land
    @Test
    public void storForbokstav_nordre_land() {
        Assertions.assertThat(
                storForbokstavStedsnavn("NORDRE LAND")
        ).isEqualTo("Nordre Land");
    }

    // AUST-AGDER skal bli Aust-Agder
    @Test
    public void storForbokstav_aust_bindestrek_agder() {
        Assertions.assertThat(
                storForbokstavStedsnavn("AUST-AGDER")
        ).isEqualTo("Aust-Agder");
    }

    // BØ (TELEMARK) skal bli Bø (Telemark)
    @Test
    public void storForbokstav_parentes() {
        Assertions.assertThat(
                storForbokstavStedsnavn("BØ (TELEMARK)")
        ).isEqualTo("Bø (Telemark)");
    }

    // MO I RANA skal bli Mo i Rana
    @Test
    public void storForbokstav_i() {
        Assertions.assertThat(
                storForbokstavStedsnavn("MO I RANA")
        ).isEqualTo("Mo i Rana");
    }

    // MØRE OG ROMSDAL skal bli Møre og Romsdal
    @Test
    public void storForbokstav_liten_og() {
        Assertions.assertThat(
                storForbokstavStedsnavn("MØRE OG ROMSDAL")
        ).isEqualTo("Møre og Romsdal");
    }
}