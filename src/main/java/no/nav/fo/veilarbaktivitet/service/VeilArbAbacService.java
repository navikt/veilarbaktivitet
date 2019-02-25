package no.nav.fo.veilarbaktivitet.service;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.SubjectHandler;
import no.nav.sbl.rest.RestUtils;
import no.nav.sbl.util.EnvironmentUtils;
import org.springframework.stereotype.Component;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static no.nav.apiapp.util.UrlUtils.clusterUrlForApplication;
import static no.nav.common.auth.SsoToken.Type.OIDC;

@Component
public class VeilArbAbacService implements Helsesjekk {

    public static final String VEILARBABAC_HOSTNAME_PROPERTY = "VEILARBABAC";

    private final String abacTargetUrl = EnvironmentUtils.getOptionalProperty(VEILARBABAC_HOSTNAME_PROPERTY)
            .orElseGet(() -> clusterUrlForApplication("veilarbabac"));

    public void sjekkLeseTilgangTilAktor(String aktorId) {
        if(!harLeseTilgangTilAktor(aktorId)){
            throw new IngenTilgang();
        }
    }

    private boolean harLeseTilgangTilAktor(String aktorId) {
        return "permit".equals(RestUtils.withClient(c -> c.target(abacTargetUrl)
                .path("self")
                .path("person")
                .queryParam("aktorId", aktorId)
                .queryParam("action", "read")
                .request()
                .header(AUTHORIZATION, "Bearer " + SubjectHandler.getSsoToken(OIDC).orElseThrow(IllegalStateException::new))
                .get(String.class)
        ));
    }

    @Override
    public void helsesjekk() {
        int status = RestUtils.withClient(c -> c.target(abacTargetUrl)
                .path("ping")
                .request()
                .get()
                .getStatus());

        if (status != 200) {
            throw new IllegalStateException("Rest kall mot veilarbabac feilet");
        }
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata(
                "veilarbabac helsesjekk",
                 abacTargetUrl,
                "Sjekker om veilarbabac endepunkt svarer",
                true
        );
    }

}
