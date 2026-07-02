package no.nav.veilarbaktivitet.aktivitet

import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType
import no.nav.veilarbaktivitet.aktivitet.domain.MoteData
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.*
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.spesialEndringer.EtikettEndring
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.spesialEndringer.ReferatEndring
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.spesialEndringer.StatusEndring
import no.nav.veilarbaktivitet.aktivitet.dto.KanalDTO
import no.nav.veilarbaktivitet.person.Innsender
import no.nav.veilarbaktivitet.service.LestAktivitetAppServiceTest
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import java.time.ZonedDateTime


class AktivitetOppdateringServiceTest: SpringBootTestBase() {

    @Autowired
    private lateinit var aktivitetOppdateringService: AktivitetOppdateringService

    @Autowired
    private lateinit var aktivitetService: AktivitetService

    private val TESTNAVIDENT = "S314159"

    @Test
    fun `Nav skal kun oppdatere detaljer når kun beskrivelse endres på avtalt møte`() {
        val moteData = MoteData.builder()
            .adresse("Adresse")
            .kanal(KanalDTO.OPPMOTE)
            .forberedelser("Forberedelser")
            .build()
        val aktivitet = aktivitetDAO.opprettNyAktivitet(AktivitetDataTestBuilder.nyMoteAktivitet()
            .withAvtalt(true)
            .withTittel("Original tittel")
            .withBeskrivelse("Original beskrivelse")
            .withMoteData(moteData))
        val oppdatertAktivitet = aktivitet.withBeskrivelse("Ny beskrivelse")
        val oppdatertAktivitetEndring = toMoteEndring(oppdatertAktivitet)

        aktivitetOppdateringService.oppdaterSomNav(oppdatertAktivitetEndring, aktivitet)

        val oppdatertEtterEndring = aktivitetDAO.hentAktivitet(aktivitet.id)
        assertThat(oppdatertEtterEndring.transaksjonsType).isEqualTo(AktivitetTransaksjonsType.AVTALT_DATO_ENDRET)
    }

    @Test
    fun `Nav skal kunne endre sluttdato selv om det er avtalt medisinsk behandling`() {
        val gammelAktivitet = AktivitetDataTestBuilder.nyBehandlingAktivitet().withAvtalt(true)
            .withTilDato(LestAktivitetAppServiceTest.toJavaUtilDate("2022-12-10"))
        val oppdatertAktivitet: AktivitetsEndring = toBehandlingEndring(
            gammelAktivitet
                .withEndretAv(TESTNAVIDENT)
                .withEndretAvType(Innsender.NAV)
                .withTilDato(LestAktivitetAppServiceTest.toJavaUtilDate("2022-12-12"))
        )
        `when`<AktivitetData?>(aktivitetService.hentAktivitetMedForhaandsorientering(oppdatertAktivitet.id)).thenReturn(
            gammelAktivitet
        )

        val endretAktivitet = aktivitetOppdateringService.oppdaterSomNav(oppdatertAktivitet, gammelAktivitet)

        Mockito.verify(aktivitetService, Mockito.times(0))
            .oppdaterAktivitet(ArgumentMatchers.any(), ArgumentMatchers.any())
        Mockito.verify(aktivitetService, Mockito.times(1))
            .oppdaterAktivitetFrist(ArgumentMatchers.any(), ArgumentMatchers.any())
        assertThat(endretAktivitet.tilDato).isNotEqualTo(gammelAktivitet.tilDato)
        assertThat(endretAktivitet.tilDato)
            .isEqualTo(LestAktivitetAppServiceTest.toJavaUtilDate("2022-12-12"))
        assertThat(endretAktivitet.endretAv).isEqualTo(TESTNAVIDENT)
        assertThat(endretAktivitet.endretAvType).isSameAs(Innsender.NAV)
    }

    private fun sporingsData(aktivitet: AktivitetData): SporingsData {
        return SporingsData(
            if (aktivitet.endretAv != null) aktivitet.endretAv else "unknown",
            if (aktivitet.endretAvType != null) aktivitet.endretAvType else Innsender.NAV,
            ZonedDateTime.now()
        )
    }

    private fun muterbareFelter(aktivitet: AktivitetData): AktivitetMuterbareFelter {
        return AktivitetMuterbareFelter(
            aktivitet.tittel,
            aktivitet.beskrivelse,
            aktivitet.fraDato,
            aktivitet.tilDato,
            aktivitet.lenke
        )
    }

    private fun opprettFelter(aktivitet: AktivitetData): AktivitetBareOpprettFelter {
        return AktivitetBareOpprettFelter(
            aktivitet.aktorId,
            aktivitet.aktivitetType,
            aktivitet.status,
            aktivitet.kontorsperreEnhetId,
            aktivitet.malid,
            ZonedDateTime.now(),
            aktivitet.isAutomatiskOpprettet,
            aktivitet.oppfolgingsperiodeId
        )
    }

    private fun toBehandlingEndring(aktivitet: AktivitetData): Behandling.Endre {
        return Behandling.Endre(
            aktivitet.id,
            aktivitet.versjon,
            muterbareFelter(aktivitet),
            sporingsData(aktivitet),
            aktivitet.behandlingAktivitetData
        )
    }

    private fun toMoteEndring(aktivitet: AktivitetData): Mote.Endre {
        return Mote.Endre(
            aktivitet.id,
            aktivitet.versjon,
            muterbareFelter(aktivitet),
            sporingsData(aktivitet),
            aktivitet.moteData
        )
    }

    private fun toMoteOpprettelse(aktivitet: AktivitetData): Mote.Opprett {
        return Mote.Opprett(
            opprettFelter(aktivitet),
            muterbareFelter(aktivitet),
            sporingsData(aktivitet),
            aktivitet.moteData
        )
    }

    private fun toIjobbEndring(aktivitet: AktivitetData): Ijobb.Endre {
        return Ijobb.Endre(
            aktivitet.id,
            aktivitet.versjon,
            muterbareFelter(aktivitet),
            sporingsData(aktivitet),
            aktivitet.iJobbAktivitetData
        )
    }

    private fun toJobbsoekingEndring(aktivitet: AktivitetData): Jobbsoeking.Endre {
        return Jobbsoeking.Endre(
            aktivitet.id,
            aktivitet.versjon,
            muterbareFelter(aktivitet),
            sporingsData(aktivitet),
            aktivitet.stillingsSoekAktivitetData
        )
    }

    private fun toStatusEndring(aktivitet: AktivitetData): StatusEndring {
        return StatusEndring(
            aktivitet.id,
            aktivitet.versjon,
            sporingsData(aktivitet),
            aktivitet.status,
            aktivitet.avsluttetKommentar
        )
    }

    private fun toEtikettEndring(aktivitet: AktivitetData): EtikettEndring {
        return EtikettEndring(
            aktivitet.id,
            aktivitet.versjon,
            sporingsData(aktivitet),
            if (aktivitet.stillingsSoekAktivitetData != null)
                aktivitet.stillingsSoekAktivitetData.stillingsoekEtikett
            else
                null
        )
    }

    private fun toReferatEndring(aktivitet: AktivitetData): ReferatEndring {
        return ReferatEndring(
            aktivitet.id,
            aktivitet.versjon,
            sporingsData(aktivitet),
            if (aktivitet.moteData != null) aktivitet.moteData else MoteData.builder().build()
        )
    }
}