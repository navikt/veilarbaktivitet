package no.nav.fo;

import no.nav.fo.veilarbaktivitet.ApplicationContext;
import org.junit.BeforeClass;
import org.junit.jupiter.api.BeforeAll;

import static java.lang.System.setProperty;
import static no.nav.fo.veilarbaktivitet.ApplicationContext.VEILARBOPPFOLGINGAPI_URL_PROPERTY;
import static no.nav.fo.veilarbaktivitet.ApplicationContext.VIRKSOMHET_TILTAK_OG_AKTIVITET_V1_ENDPOINTURL_PROPERTY;

public abstract class IntegrasjonsTest extends AbstractIntegrasjonsTest {

    static {
        setProperty(VEILARBOPPFOLGINGAPI_URL_PROPERTY, "https://localhost");
        setProperty(VIRKSOMHET_TILTAK_OG_AKTIVITET_V1_ENDPOINTURL_PROPERTY, "https://localhost");
    }

    @BeforeAll
    @BeforeClass
    public static void setupContext() {
        setupContext(
                ApplicationContext.class,
                IntegrasjonsTest.Request.class
        );
    }

}
