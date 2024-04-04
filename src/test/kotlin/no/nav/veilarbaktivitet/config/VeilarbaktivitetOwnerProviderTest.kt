package no.nav.veilarbaktivitet.config

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VeilarbaktivitetOwnerProviderTest {

    @Test
    fun test_klasse_sjekk() {
        val resourceType = ArenaAktivitetResource::class
        assertTrue(resourceType == ArenaAktivitetResource::class)
    }

}