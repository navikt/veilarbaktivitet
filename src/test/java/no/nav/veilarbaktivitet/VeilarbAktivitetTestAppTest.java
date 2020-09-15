package no.nav.veilarbaktivitet;

import no.nav.veilarbaktivitet.mock.LocalH2Database;
import org.junit.Test;

import java.lang.reflect.Array;

public class VeilarbAktivitetTestAppTest {
    @Test
    public void smokeTest()  {
        LocalH2Database.setUseInnMemmory();
        System.setProperty("server.port", "0");

        VeilarbAktivitetTestApp.main(new String[0]);
    }
}
