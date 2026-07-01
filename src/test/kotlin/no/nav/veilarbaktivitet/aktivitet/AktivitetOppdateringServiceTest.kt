package no.nav.veilarbaktivitet.aktivitet

import no.nav.poao.dab.spring_auth.IAuthService
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.MoteData
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.*
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.spesialEndringer.EtikettEndring
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.spesialEndringer.ReferatEndring
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.spesialEndringer.StatusEndring
import no.nav.veilarbaktivitet.aktivitet.dto.KanalDTO
import no.nav.veilarbaktivitet.person.Innsender
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.ZonedDateTime

@ExtendWith(MockitoExtension::class)
class AktivitetOppdateringServiceTest {

    @Mock
    private lateinit var aktivitetService: AktivitetService

    @InjectMocks
    private lateinit var aktivitetOppdateringService: AktivitetOppdateringService

    @Test
    fun nav_skal_kun_oppdatere_detaljer_naar_kun_beskrivelse_endres_paa_avtalt_mote() {
        val moteData = MoteData.builder()
            .adresse("Adresse")
            .kanal(KanalDTO.OPPMOTE)
            .forberedelser("Forberedelser")
            .build()
        val gammelAktivitet: AktivitetData = AktivitetDataTestBuilder.nyMoteAktivitet()
            .withAvtalt(true)
            .withTittel("Original tittel")
            .withBeskrivelse("Original beskrivelse")
            .withMoteData(moteData)
        val oppdatertAktivitet = gammelAktivitet.withBeskrivelse("Ny beskrivelse")
        val oppdatertAktivitetEndring = toMoteEndring(oppdatertAktivitet)

        `when`<AktivitetData?>(aktivitetService.hentAktivitetMedForhaandsorientering(oppdatertAktivitet.getId()))
            .thenReturn(gammelAktivitet)

        aktivitetOppdateringService.oppdaterSomNav(oppdatertAktivitetEndring, gammelAktivitet)

        Mockito.verify(aktivitetService, Mockito.times(0))
            .oppdaterMoteTidStedOgKanal(ArgumentMatchers.any(), ArgumentMatchers.any())
        Mockito.verify(aktivitetService, Mockito.times(1))
            .oppdaterMoteDetaljer(ArgumentMatchers.any(), ArgumentMatchers.any())
        Mockito.verify(aktivitetService, Mockito.times(0))
            .oppdaterAktivitet(ArgumentMatchers.any(), ArgumentMatchers.any())
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