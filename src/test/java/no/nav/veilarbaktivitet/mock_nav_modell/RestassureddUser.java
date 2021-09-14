package no.nav.veilarbaktivitet.mock_nav_modell;

import io.restassured.specification.RequestSpecification;
import no.nav.common.auth.context.UserRole;
import no.nav.veilarbaktivitet.config.TestAuthContextFilter;

import static io.restassured.RestAssured.given;

class RestassureddUser {
    final String ident;
    final UserRole userRole;

    RestassureddUser(String ident, UserRole userRole) {
        this.ident = ident;
        this.userRole = userRole;
    }

    public RequestSpecification createRequest() {
        return given()
                .header("Content-type", "application/json")
                .header(TestAuthContextFilter.identHeder, ident)
                .header(TestAuthContextFilter.typeHeder, userRole);
    }
}
