package no.nav.veilarbaktivitet.util;

import io.restassured.response.ValidatableResponse;
import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;

@RequiredArgsConstructor
public class KasserTestService {
    private final int port;
    public ValidatableResponse kasserAktivitet(MockVeileder veileder, long aktivitetId) {
        return veileder
                .createRequest()
                .and()
                .when()
                .put("http://localhost:" + port + "/veilarbaktivitet/api/kassering/" + aktivitetId)
                .then();
    }
}
