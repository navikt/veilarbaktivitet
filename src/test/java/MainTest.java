import no.nav.fo.veilarbaktivitet.TestContext;
import no.nav.testconfig.ApiAppTest;

import static no.nav.fo.veilarbaktivitet.ApplicationContext.APPLICATION_NAME;

public class MainTest {

    public static void main(String[] args) {
        ApiAppTest.setupTestContext(ApiAppTest.Config.builder().applicationName(APPLICATION_NAME).build());
        TestContext.setup();
        Main.main("8480","8481");
    }
}
