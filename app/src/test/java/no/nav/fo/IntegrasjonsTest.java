package no.nav.fo;

import no.nav.fo.veilarbaktivitet.ApplicationContext;
import no.nav.modig.testcertificates.TestCertificates;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.PropertySource;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import static no.nav.fo.veilarbaktivitet.db.DatabaseContext.AKTIVITET_DATA_SOURCE_JDNI_NAME;

@ContextConfiguration(classes = {
        ApplicationContext.class,
        IntegrasjonsTest.JndiBean.class,
        IntegrasjonsTest.Request.class
})
@RunWith(SpringJUnit4ClassRunner.class)
@PropertySource("classpath:test.properties")
@Transactional
public abstract class IntegrasjonsTest {

    @BeforeClass
    public static void testCertificates() {
        TestCertificates.setupKeyAndTrustStore();
    }

    @BeforeClass
    public static void testProperties() throws IOException {
        System.getProperties().load(IntegrasjonsTest.class.getResourceAsStream("/test.properties"));
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
