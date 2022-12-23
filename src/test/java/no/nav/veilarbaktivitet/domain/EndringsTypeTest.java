package no.nav.veilarbaktivitet.domain;

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.veilarbportefolje.EndringsType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EndringsTypeTest {
    @Test
    public void alleVerdierSkalBliMappetUtenError() {
        AktivitetTransaksjonsType[] values = AktivitetTransaksjonsType.values();
        for (AktivitetTransaksjonsType value : values) {
            EndringsType endringsType = EndringsType.get(value);
            assertThat(endringsType).isNotNull();
        }
    }
}
