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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class VeilArbAbacServiceTest {

    private static final String AKTOR_ID = "test-aktorId";
    private static final String FNR = "test-fnr";
    private static final String ENHET = "test-enhet";

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
    public void leseTilgangTilAktor() {
        givenThat(get(urlEqualTo("/self/person?aktorId=test-aktorId&action=read"))
                .willReturn(aResponse().withStatus(200).withBody("deny"))
        );
        assertThatThrownBy(() -> veilArbAbacService.sjekkLeseTilgangTilAktor(AKTOR_ID)).isInstanceOf(IngenTilgang.class);

        givenThat(get(urlEqualTo("/self/person?aktorId=test-aktorId&action=read"))
                .willReturn(aResponse().withStatus(200).withBody("permit"))
        );
        veilArbAbacService.sjekkLeseTilgangTilAktor(AKTOR_ID);
    }


    @Test
    public void skriveTilgangTilAktor() {
        givenThat(get(urlEqualTo("/self/person?aktorId=test-aktorId&action=update"))
                .willReturn(aResponse().withStatus(200).withBody("deny"))
        );
        assertThatThrownBy(() -> veilArbAbacService.sjekkSkriveTilgangTilAktor(AKTOR_ID)).isInstanceOf(IngenTilgang.class);

        givenThat(get(urlEqualTo("/self/person?aktorId=test-aktorId&action=update"))
                .willReturn(aResponse().withStatus(200).withBody("permit"))
        );
        veilArbAbacService.sjekkSkriveTilgangTilAktor(AKTOR_ID);
    }

    @Test
    public void leseTilgangTilFnr() {
        givenThat(get(urlEqualTo("/self/person?fnr=test-fnr&action=read"))
                .willReturn(aResponse().withStatus(200).withBody("deny"))
        );
        assertThatThrownBy(() -> veilArbAbacService.sjekkLeseTilgangTilFnr(FNR)).isInstanceOf(IngenTilgang.class);

        givenThat(get(urlEqualTo("/self/person?fnr=test-fnr&action=read"))
                .willReturn(aResponse().withStatus(200).withBody("permit"))
        );
        veilArbAbacService.sjekkLeseTilgangTilFnr(FNR);
    }

    @Test
    public void leseTilgangTilEnhet() {
        givenThat(get(urlEqualTo("/self/enhet?enhetId=test-enhet&action=read"))
                .willReturn(aResponse().withStatus(200).withBody("deny"))
        );
        assertThat(veilArbAbacService.harTilgangTilEnhet(ENHET)).isFalse();

        givenThat(get(urlEqualTo("/self/enhet?enhetId=test-enhet&action=read"))
                .willReturn(aResponse().withStatus(200).withBody("permit"))
        );
        assertThat(veilArbAbacService.harTilgangTilEnhet(ENHET)).isTrue();
    }

}