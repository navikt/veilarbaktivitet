package no.nav.fo;

import no.nav.fasit.FasitUtils;
import no.nav.fo.veilarbaktivitet.ApplicationContext;
import no.nav.sbl.dialogarena.test.FasitAssumption;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.jupiter.api.BeforeAll;

import static java.lang.System.setProperty;
import static no.nav.fo.veilarbaktivitet.ApplicationContext.VEILARBOPPFOLGINGAPI_URL_PROPERTY;
import static no.nav.fo.veilarbaktivitet.ApplicationContext.VIRKSOMHET_TILTAKOGAKTIVITET_V1_ENDPOINTURL_PROPERTY;

public abstract class IntegrasjonsTest extends AbstractIntegrasjonsTest {

    static {
        setProperty(VEILARBOPPFOLGINGAPI_URL_PROPERTY, "https://localhost");
        setProperty(VIRKSOMHET_TILTAKOGAKTIVITET_V1_ENDPOINTURL_PROPERTY, "https://localhost");
    }

    @BeforeAll
    @BeforeClass
    public static void setupContext() {

        FasitAssumption.assumeFasitAccessible();
        Assume.assumeFalse(FasitUtils.usingMock());

        setupContext(
                ApplicationContext.class,
                IntegrasjonsTest.Request.class
        );
    }

}
