package no.nav.veilarbaktivitet.domain;

import static org.assertj.core.api.Assertions.assertThat;

import no.nav.veilarbaktivitet.aktiviterTilKafka.EndringsType;
import org.junit.Test;

public class EndringsTypeTest {

	@Test
	public void alleVerdierSkalBliMappetUtenError() {
		AktivitetTransaksjonsType[] values = AktivitetTransaksjonsType.values();
		for (AktivitetTransaksjonsType value : values) {
			EndringsType endringsType = EndringsType.get(value);
			assertThat(endringsType).isNotNull();
		}
	}
}
