package no.nav.veilarbaktivitet.oversikten

import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

open class OversiktenMeldingMedMetadataDAOTest: SpringBootTestBase() {

    @Autowired
    lateinit var oversiktenMeldingMedMetadataDAO: OversiktenMeldingMedMetadataDAO

    @Test
    fun `UdeltSamtalereferat IkkeSendtMelding ReturnererEttResultat`() {
        val happyBruker = navMockService.createBruker()
        val veileder = navMockService.createVeileder(happyBruker)
        val aktivitet = aktivitetTestService.opprettAktivitet(
            happyBruker,
            veileder,
            AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE).setErReferatPublisert(false).setReferat("Et referat")
        )

        val resultat = oversiktenMeldingMedMetadataDAO.hentUdelteSamtalereferatDerViIkkeHarSendtMeldingTilOversikten()
        assertThat(resultat).hasSize(1)
        assertThat(resultat.first().aktorId).isEqualTo(happyBruker.aktorId)
        assertThat(resultat.first().aktivitetId).isEqualTo(aktivitet.id)

    }
}