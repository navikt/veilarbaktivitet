package no.nav.veilarbaktivitet.arkivering.etiketter

import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Etikett
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EtikettExtractorTest {

    @Test
    fun `Skal mappe arena-etiketter riktig`() {
        assertThat(Etikett("Dummy", null, "SOKT_INN").mapTilArenaEtikett()).isEqualTo(ArkivEtikett(ArkivEtikettStil.POSITIVE, "Søkt inn på tiltaket"))
        assertThat(Etikett("Dummy", null, "AVSLAG").mapTilArenaEtikett()).isEqualTo(ArkivEtikett(ArkivEtikettStil.NEGATIVE, "Fått avslag"))
        assertThat(Etikett("Dummy", null, "IKKE_AKTUELL").mapTilArenaEtikett()).isEqualTo(ArkivEtikett(ArkivEtikettStil.NEUTRAL, "Ikke aktuell for tiltaket"))
        assertThat(Etikett("Dummy", null, "IKKE_MOETT").mapTilArenaEtikett()).isEqualTo(ArkivEtikett(ArkivEtikettStil.NEGATIVE, "Ikke møtt på tiltaket"))
        assertThat(Etikett("Dummy", null, "INFOMOETE").mapTilArenaEtikett()).isEqualTo(ArkivEtikett(ArkivEtikettStil.POSITIVE, "Infomøte før tiltaket"))
        assertThat(Etikett("Dummy", null, "TAKKET_JA").mapTilArenaEtikett()).isEqualTo(ArkivEtikett(ArkivEtikettStil.POSITIVE, "Takket ja til tilbud"))
        assertThat(Etikett("Dummy", null, "TAKKET_NEI").mapTilArenaEtikett()).isEqualTo(ArkivEtikett(ArkivEtikettStil.NEUTRAL, "Takket nei til tilbud"))
        assertThat(Etikett("Dummy", null, "FATT_PLASS").mapTilArenaEtikett()).isEqualTo(ArkivEtikett(ArkivEtikettStil.POSITIVE, "Fått plass på tiltaket"))
        assertThat(Etikett("Dummy", null, "VENTELISTE").mapTilArenaEtikett()).isEqualTo(ArkivEtikett(ArkivEtikettStil.POSITIVE, "På venteliste"))
    }
}
