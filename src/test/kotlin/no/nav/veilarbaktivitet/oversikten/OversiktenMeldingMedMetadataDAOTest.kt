package no.nav.veilarbaktivitet.oversikten

import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

open class OversiktenMeldingMedMetadataDAOTest: SpringBootTestBase() {

    @Autowired
    lateinit var oversiktenService: OversiktenService
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
        assertThat(resultat.first().aktorId.get()).isEqualTo(happyBruker.aktorId.get())
        assertThat(resultat.first().aktivitetId.toString()).isEqualTo(aktivitet.id)

    }

    @Test
    fun `UdeltSamtalereferat SendtMelding ReturnererIngen`() {
        val happyBruker = navMockService.createBruker()
        val veileder = navMockService.createVeileder(happyBruker)
        val aktivitet = aktivitetTestService.opprettAktivitet(
            happyBruker,
            veileder,
            AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE).setErReferatPublisert(false).setReferat("Et referat")
        )
        oversiktenService.lagreStartMeldingOmUdeltSamtalereferatIUtboks(happyBruker.aktorId, aktivitet.id.toLong())
        oversiktenService.sendUsendteMeldingerTilOversikten()

        val resultat = oversiktenMeldingMedMetadataDAO.hentUdelteSamtalereferatDerViIkkeHarSendtMeldingTilOversikten()

        assertThat(resultat).isEmpty()

    }

}