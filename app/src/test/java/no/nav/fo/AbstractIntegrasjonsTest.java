package no.nav.fo;

import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.ServiceUser;
import no.nav.modig.testcertificates.TestCertificates;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.PropertySource;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

import static no.nav.fo.veilarbaktivitet.db.DatabaseContext.AKTIVITET_DATA_SOURCE_JDNI_NAME;
import static org.apache.cxf.staxutils.StaxUtils.ALLOW_INSECURE_PARSER;

@RunWith(SpringJUnit4ClassRunner.class)
@PropertySource("classpath:test.properties")
@Transactional
public abstract class AbstractIntegrasjonsTest {

    @BeforeClass
    public static void serviceUser() throws IOException {
        DevelopmentSecurity.setupIntegrationTestSecurity(FasitUtils.getServiceUser("srvveilarbaktivitet", "veilarbaktivitet", "t6"));
    }

    @BeforeClass
    public static void testProperties() throws IOException {
        System.getProperties().load(AbstractIntegrasjonsTest.class.getResourceAsStream("/integrasjonstest.properties"));
    }

    @Component
    public static class JndiBean {

        public JndiBean() throws Exception {
            SimpleNamingContextBuilder builder = new SimpleNamingContextBuilder();
            builder.bind(AKTIVITET_DATA_SOURCE_JDNI_NAME, DatabaseTestContext.buildDataSource());
            builder.activate();
        }

    }

    @Component
    public static class Request extends MockHttpServletRequest {}

}
