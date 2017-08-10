package no.nav.fo;

import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.sbl.dialogarena.common.jetty.Jetty;

import static no.nav.fo.DatabaseTestContext.buildDataSourceFor;
import static no.nav.fo.veilarbaktivitet.db.DatabaseContext.AKTIVITET_DATA_SOURCE_JDNI_NAME;
import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;
import static no.nav.sbl.dialogarena.common.jetty.JettyStarterUtils.*;

public class StartJetty {
    private static final int PORT = 8481;
    private static final int SSL_PORT = 8482;

    public static void main(String[] args) throws Exception {
        Jetty jetty = DevelopmentSecurity.setupSamlLogin(usingWar()
                .at("/veilarbaktivitet-ws")
                .loadProperties("/test.properties")
                .addDatasource(buildDataSourceFor(System.getProperty("database")), AKTIVITET_DATA_SOURCE_JDNI_NAME)
                .port(PORT)
                .sslPort(SSL_PORT),  new DevelopmentSecurity.SamlSecurityConfig("veilarbaktivitet", "t6")
            ).buildJetty();
        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }

}
