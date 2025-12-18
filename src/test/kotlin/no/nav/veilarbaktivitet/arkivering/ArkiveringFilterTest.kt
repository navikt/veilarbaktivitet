package no.nav.veilarbaktivitet.arkivering

import no.nav.veilarbaktivitet.arena.model.ArenaStatusEtikettDTO
import no.nav.veilarbaktivitet.arkivering.ArkiveringsController.AvtaltMedNavFilter.AVTALT_MED_NAV
import no.nav.veilarbaktivitet.arkivering.ArkiveringsController.AvtaltMedNavFilter.IKKE_AVTALT_MED_NAV
import no.nav.veilarbaktivitet.arkivering.ArkiveringsController.KvpUtvalgskriterieAlternativ.INKLUDER_KVP_AKTIVITETER
import no.nav.veilarbaktivitet.arkivering.ArkiveringsController.SøknadsstatusFilter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ArkiveringFilterTest {

    @Test
    fun `Ubrukt filter skal ikke inkluderes i brukteFiltre`() {
        val tomtFilter = defaultFilter
        assertThat(tomtFilter.mapTilBrukteFiltre()).isEmpty()
    }

    @Test
    fun `Skal mappe avtaltMedNavFilter riktig`() {
        val filter = defaultFilter.copy(aktivitetAvtaltMedNavFilter = listOf(AVTALT_MED_NAV, IKKE_AVTALT_MED_NAV))
        val brukteFiltre = filter.mapTilBrukteFiltre()
        assertThat(brukteFiltre).containsKey("Avtalt med Nav")
        assertThat(brukteFiltre["Avtalt med Nav"]).containsExactly("Avtalt med Nav", "Ikke avtalt med Nav")
    }

    @Test
    fun `Skal mappe stillingsstatusFilter riktig`() {
        val filter = defaultFilter.copy(stillingsstatusFilter = listOf(SøknadsstatusFilter.SOKNAD_SENDT,
            SøknadsstatusFilter.FATT_JOBBEN))
        val brukteFiltre = filter.mapTilBrukteFiltre()
        assertThat(brukteFiltre).containsKey("Stillingsstatus")
        assertThat(brukteFiltre["Stillingsstatus"]).containsExactly("Søknad sendt", "Fått jobben")
    }

    @Test
    fun `Skal mappe arenaAktivitetStatusFilter riktig`() {
        val filter = defaultFilter.copy(arenaAktivitetStatusFilter = listOf(ArenaStatusEtikettDTO.AKTUELL,
            ArenaStatusEtikettDTO.JATAKK))
        val brukteFiltre = filter.mapTilBrukteFiltre()
        assertThat(brukteFiltre).containsKey("Status for Arena-aktivitet")
        assertThat(brukteFiltre["Status for Arena-aktivitet"]).containsExactly("Søkt inn på tiltaket", "Takket ja til tilbud")
    }

    @Test
    fun `Skal mappe aktivitetTypeFilter riktig`() {
        val filter = defaultFilter.copy(aktivitetTypeFilter = listOf(ArkiveringsController.AktivitetTypeFilter.ARENA_TILTAK, ArkiveringsController.AktivitetTypeFilter.SAMTALEREFERAT))
        val brukteFiltre = filter.mapTilBrukteFiltre()
        assertThat(brukteFiltre).containsKey("Aktivitetstype")
        assertThat(brukteFiltre["Aktivitetstype"]).containsExactly("Tiltak gjennom Nav", "Samtalereferat")
    }

    private val defaultFilter = ArkiveringsController.Filter(
        inkluderHistorikk = true,
        aktivitetAvtaltMedNavFilter = emptyList(),
        stillingsstatusFilter = emptyList(),
        arenaAktivitetStatusFilter = emptyList(),
        aktivitetTypeFilter = emptyList(),
        inkluderDialoger = true,
        kvpUtvalgskriterie = ArkiveringsController.KvpUtvalgskriterie(
            alternativ = INKLUDER_KVP_AKTIVITETER,
            start = null,
            slutt = null
        )
    )
}
