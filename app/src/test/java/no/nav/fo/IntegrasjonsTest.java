package no.nav.fo;

import no.nav.fo.veilarbaktivitet.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {
        ApplicationContext.class,
        IntegrasjonsTest.JndiBean.class,
        IntegrasjonsTest.Request.class
})
public abstract class IntegrasjonsTest extends AbstractIntegrasjonsTest {}
