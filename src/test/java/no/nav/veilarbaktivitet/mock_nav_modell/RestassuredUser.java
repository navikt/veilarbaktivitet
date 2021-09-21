package no.nav.veilarbaktivitet.mock_nav_modell;

import io.restassured.specification.RequestSpecification;
import no.nav.common.auth.context.UserRole;
import no.nav.veilarbaktivitet.config.TestAuthContextFilter;

import static io.restassured.RestAssured.given;

public class RestassuredUser {
    final String ident;
    final UserRole userRole;

    RestassuredUser(String ident, UserRole userRole) {
        this.ident = ident;
        this.userRole = userRole;
    }

    public RequestSpecification createRequest() {
        return given()
                .header("Content-type", "application/json")
                .header(TestAuthContextFilter.identHeder, ident)
                .header(TestAuthContextFilter.typeHeder, userRole);
    }

    public String getUrl(String baseUrl, MockBruker mockBruker) {
        if (userRole.equals(UserRole.EKSTERN)) {
            return baseUrl;
        }
        return baseUrl + "?fnr=" + mockBruker.getFnr();
    }
}
