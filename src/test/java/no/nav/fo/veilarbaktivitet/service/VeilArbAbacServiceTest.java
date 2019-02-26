package no.nav.fo.veilarbaktivitet.service;


import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.brukerdialog.security.context.SubjectRule;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.auth.Subject;
import no.nav.sbl.dialogarena.test.junit.SystemPropertiesRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.net.ConnectException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.Collections.emptyMap;
import static no.nav.common.auth.SsoToken.oidcToken;
import static no.nav.fo.veilarbaktivitet.service.VeilArbAbacService.VEILARBABAC_HOSTNAME_PROPERTY;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class VeilArbAbacServiceTest {

    private static final String AKTOR_ID = "aktorId";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Rule
    public SystemPropertiesRule systemPropertiesRule = new SystemPropertiesRule();

    @Rule
    public SubjectRule subjectRule = new SubjectRule(new Subject("test-subject", IdentType.EksternBruker, oidcToken("test", emptyMap())));

    private VeilArbAbacService veilArbAbacService;

    @Before
    public void setup() {
        systemPropertiesRule.setProperty(VEILARBABAC_HOSTNAME_PROPERTY, "http://localhost:" + wireMockRule.port());
        veilArbAbacService = new VeilArbAbacService();
    }

    @Test
    public void ping() {
        givenThat(get(urlEqualTo("/ping"))
                .willReturn(aResponse().withStatus(200))
        );
        veilArbAbacService.helsesjekk();

        givenThat(get(urlEqualTo("/ping"))
                .willReturn(aResponse().withStatus(500))
        );
        assertThatThrownBy(() -> veilArbAbacService.helsesjekk()).isInstanceOf(IllegalStateException.class);

        wireMockRule.stop();
        assertThatThrownBy(() -> veilArbAbacService.helsesjekk()).hasCauseInstanceOf(ConnectException.class);
    }

    @Test
    public void harVeilederSkriveTilgangTilFnr() {
        givenThat(get(urlEqualTo("/self/person?aktorId=aktorId&action=read"))
                .willReturn(aResponse().withStatus(200).withBody("deny"))
        );
        assertThatThrownBy(() -> veilArbAbacService.sjekkLeseTilgangTilAktor(AKTOR_ID)).isInstanceOf(IngenTilgang.class);

        givenThat(get(urlEqualTo("/self/person?aktorId=aktorId&action=read"))
                .willReturn(aResponse().withStatus(200).withBody("permit"))
        );
        veilArbAbacService.sjekkLeseTilgangTilAktor(AKTOR_ID);
    }

}