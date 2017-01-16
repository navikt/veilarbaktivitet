package no.nav.fo;

import no.nav.sbl.dialogarena.common.jetty.Jetty;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import static no.nav.fo.veilarbaktivitet.db.DatabaseContext.AKTIVITET_DATA_SOURCE_JDNI_NAME;
import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;
import static no.nav.sbl.dialogarena.common.jetty.JettyStarterUtils.*;

public class StartJetty {
    private static final int PORT = 8486;

    public static void main(String[] args) throws Exception {
        Jetty jetty = usingWar()
                .at("/veilarbaktivitet")
                .loadProperties("/test.properties")
                .addDatasource(DatabaseTestContext.buildDataSource(), AKTIVITET_DATA_SOURCE_JDNI_NAME)
                .port(PORT)
                .buildJetty();
        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }

}
