import no.nav.fo.veilarbaktivitet.TestContext;
import no.nav.testconfig.ApiAppTest;

import static no.nav.fo.veilarbaktivitet.ApplicationContext.APPLICATION_NAME;

public class MainTest {

    private static final String PORT = "8480";

    public static void main(String[] args) {
        ApiAppTest.setupTestContext(ApiAppTest.Config.builder().applicationName(APPLICATION_NAME).build());
        TestContext.setup();
        Main.main(PORT);
    }
}
