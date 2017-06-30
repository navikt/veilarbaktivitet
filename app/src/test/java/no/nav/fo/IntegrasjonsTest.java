package no.nav.fo;

import no.nav.fo.veilarbaktivitet.ApplicationContext;
import org.junit.BeforeClass;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.ContextConfiguration;

public abstract class IntegrasjonsTest extends AbstractIntegrasjonsTest {

    @BeforeAll
    @BeforeClass
    public static void setupContext() {
        setupContext(
                ApplicationContext.class,
                IntegrasjonsTest.JndiBean.class,
                IntegrasjonsTest.Request.class
        );
    }

}
