package no.nav.fo;

import no.nav.modig.core.context.JettySubjectHandler;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import org.apache.geronimo.components.jaspi.AuthConfigFactoryImpl;

import javax.security.auth.message.config.AuthConfigFactory;

import static java.lang.System.setProperty;
import static no.nav.fo.veilarbaktivitet.db.DatabaseContext.AKTIVITET_DATA_SOURCE_JDNI_NAME;
import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;
import static no.nav.sbl.dialogarena.common.jetty.JettyStarterUtils.*;

public class StartJetty {
    public static final String CONTEXT_NAME = "veilarbaktivitet";
    public static final int PORT = 8480;
    private static final int SSL_PORT = 8481;

    public static void main(String[] args) throws Exception {
        setupAutentisering();
        Jetty jetty = usingWar()
                .at(CONTEXT_NAME)
                .loadProperties("/test.properties")
                .addDatasource(DatabaseTestContext.buildDataSource(), AKTIVITET_DATA_SOURCE_JDNI_NAME)
                .port(PORT)
                .sslPort(SSL_PORT)
                .configureForJaspic()
                .buildJetty();
        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }

    private static void setupAutentisering() {
        setProperty("develop-local", "true");
        setProperty("no.nav.modig.core.context.subjectHandlerImplementationClass", JettySubjectHandler.class.getName());
        setProperty("org.apache.geronimo.jaspic.configurationFile", "web/src/test/resources/jaspiconf.xml");
        setProperty(AuthConfigFactory.DEFAULT_FACTORY_SECURITY_PROPERTY, AuthConfigFactoryImpl.class.getCanonicalName());
    }

}
