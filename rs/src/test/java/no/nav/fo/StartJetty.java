package no.nav.fo;

import no.nav.dialogarena.config.DevelopmentSecurity.ISSOSecurityConfig;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.TestEnvironment;
import no.nav.sbl.dialogarena.common.jetty.Jetty;

import static no.nav.apiapp.util.StringUtils.of;
import static no.nav.dialogarena.config.DevelopmentSecurity.setupISSO;
import static no.nav.fo.veilarbaktivitet.db.DatabaseContext.AKTIVITET_DATA_SOURCE_JDNI_NAME;
import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;
import static no.nav.sbl.dialogarena.common.jetty.JettyStarterUtils.*;

public class StartJetty {

    public static final String CONTEXT_NAME = "veilarbaktivitet";
    public static final int PORT = 8480;
    private static final int SSL_PORT = 8481;

    public static void main(String[] args) throws Exception {
        Jetty jetty = setupISSO(usingWar()
                .at(CONTEXT_NAME)
                .loadProperties("/test.properties")
                .addDatasource(of(System.getProperty("database"))
                        .map(TestEnvironment::valueOf)
                        .map(testEnvironment -> FasitUtils.getDbCredentials(testEnvironment, CONTEXT_NAME))
                        .map(DatabaseTestContext::build)
                        .orElseGet(DatabaseTestContext::buildDataSource), AKTIVITET_DATA_SOURCE_JDNI_NAME)
                .port(PORT)
                .sslPort(SSL_PORT)
        , new ISSOSecurityConfig(CONTEXT_NAME,"t6")).buildJetty();
        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }

}
