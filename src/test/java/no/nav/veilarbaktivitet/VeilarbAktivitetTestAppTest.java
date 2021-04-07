package no.nav.veilarbaktivitet;

import java.lang.reflect.Array;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import org.junit.Test;

public class VeilarbAktivitetTestAppTest {

	@Test
	public void smokeTest() {
		LocalH2Database.setUseInnMemmory();
		System.setProperty("server.port", "0");

		VeilarbAktivitetTestApp.main(new String[0]);
	}
}
