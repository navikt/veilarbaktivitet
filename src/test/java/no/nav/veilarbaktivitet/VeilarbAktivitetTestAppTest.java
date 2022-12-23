package no.nav.veilarbaktivitet;

import no.nav.veilarbaktivitet.mock.LocalH2Database;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Array;

public class VeilarbAktivitetTestAppTest {

    @Test
    public void smokeTest()  {
        LocalH2Database.setUseInnMemmory();
        System.setProperty("server.port", "0");

        Assertions.assertThatCode(() ->VeilarbAktivitetTestApp.main(new String[0])).doesNotThrowAnyException();
    }
}
