package no.nav.veilarbaktivitet.aktivitet

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.poao.dab.spring_auth.IAuthService
import no.nav.veilarbaktivitet.aktivitet.domain.*
import no.nav.veilarbaktivitet.aktivitet.dto.*
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDataMapperService
import no.nav.veilarbaktivitet.aktivitetskort.MigreringService
import no.nav.veilarbaktivitet.avtalt_med_nav.AvtaltMedNavService
import no.nav.veilarbaktivitet.eventsLogger.BigQueryClient
import no.nav.veilarbaktivitet.kvp.KvpService
import no.nav.veilarbaktivitet.mock.TestData
import no.nav.veilarbaktivitet.oppfolging.periode.Oppfolgingsperiode
import no.nav.veilarbaktivitet.oppfolging.periode.OppfolgingsperiodeService
import no.nav.veilarbaktivitet.oppfolging.periode.SistePeriodeService
import no.nav.veilarbaktivitet.oversikten.OversiktenService
import no.nav.veilarbaktivitet.person.Innsender
import no.nav.veilarbaktivitet.person.PersonService
import no.nav.veilarbaktivitet.person.UserInContext
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.web.server.ResponseStatusException
import java.time.ZonedDateTime
import java.util.*

/**
 * Test suite som verifiserer hele flyten fra Controller -> MapperService -> AppService -> AktivitetService -> AktivitetDAO (mockes).
 * Ingen ekte database brukes. Vi verifiserer at riktig AktivitetData sendes til AktivitetDAO.
 *
 * Alle tester bruker eksplisitte, unike verdier per felt for å fange feil der felter
 * byttes om ved mapping (f.eks. tittel -> beskrivelse).
 */
@ExtendWith(MockitoExtension::class)
class AktivitetOpprettOgOppdaterFlowTest {

    @Mock lateinit var aktivitetDAO: AktivitetDAO
    @Mock lateinit var avtaltMedNavService: AvtaltMedNavService
    @Mock lateinit var metricService: MetricService
    @Mock lateinit var sistePeriodeService: SistePeriodeService
    @Mock lateinit var oversiktenService: OversiktenService
    @Mock lateinit var authService: IAuthService
    @Mock lateinit var personService: PersonService
    @Mock lateinit var bigQueryClient: BigQueryClient
    @Mock lateinit var aktorOppslagClient: AktorOppslagClient
    @Mock lateinit var userInContext: UserInContext
    @Mock lateinit var kvpService: KvpService
    @Mock lateinit var oppfolgingsperiodeService: OppfolgingsperiodeService
    @Mock lateinit var migreringService: MigreringService

    private lateinit var aktivitetService: AktivitetService
    private lateinit var appService: AktivitetAppService
    private lateinit var mapperService: AktivitetDataMapperService
    private lateinit var controller: AktivitetsplanController

    private val oppfolgingsperiodeId = UUID.randomUUID()
    private val aktorId = TestData.KJENT_AKTOR_ID
    private val navIdent = NavIdent.of("Z999999")

    // Eksplisitte unike datoer for å unngå at fraDato og tilDato har samme verdi
    private val fraDato = Date(1700000000000L) // 2023-11-14
    private val tilDato = Date(1800000000000L) // 2027-01-15

    @BeforeEach
    fun setup() {
        aktivitetService = AktivitetService(aktivitetDAO, avtaltMedNavService, metricService, sistePeriodeService, oversiktenService)
        appService = AktivitetAppService(authService, aktivitetService, metricService, personService, bigQueryClient, oversiktenService)
        mapperService = AktivitetDataMapperService(authService, aktorOppslagClient, userInContext, kvpService, oppfolgingsperiodeService)
        controller = AktivitetsplanController(authService, appService, mapperService, userInContext, migreringService, bigQueryClient)
    }

    private fun setupNavBrukerContext() {
        `when`(authService.getLoggedInnUser()).thenReturn(navIdent)
        `when`(authService.erEksternBruker()).thenReturn(false)
        `when`(authService.erSystemBruker()).thenReturn(false)
        `when`(userInContext.getAktorId()).thenReturn(aktorId)
        `when`(kvpService.getKontorSperreEnhet(aktorId)).thenReturn(Optional.empty())
        `when`(oppfolgingsperiodeService.hentNåværendeÅpenPeriode(aktorId)).thenReturn(
            Oppfolgingsperiode(aktorId.get(), oppfolgingsperiodeId, ZonedDateTime.now(), null)
        )
    }

    private fun setupNavBrukerContextForOppdater() {
        `when`(authService.erInternBruker()).thenReturn(true)
        `when`(authService.getLoggedInnUser()).thenReturn(navIdent)
    }

    private fun setupEksternBrukerContextForOppdater() {
        val eksternAktorId = no.nav.common.types.identer.AktorId.of(aktorId.get())
        `when`(authService.getLoggedInnUser()).thenReturn(Fnr.of("12345678901"))
        `when`(authService.erEksternBruker()).thenReturn(true)
        `when`(authService.erInternBruker()).thenReturn(false)
        `when`(aktorOppslagClient.hentAktorId(any<Fnr>())).thenReturn(eksternAktorId)
    }

    private fun stubOpprettNyAktivitetReturnsInput() {
        `when`(aktivitetDAO.opprettNyAktivitet(any())).thenAnswer { invocation ->
            (invocation.getArgument(0) as AktivitetData).toBuilder().id(1L).versjon(1L).build()
        }
    }

    private fun captureOpprettNyAktivitet(): AktivitetData {
        val captor = ArgumentCaptor.forClass(AktivitetData::class.java)
        verify(aktivitetDAO).opprettNyAktivitet(captor.capture())
        return captor.value
    }

    private fun stubHentOgOppdater(eksisterende: AktivitetData) {
        `when`(aktivitetDAO.hentAktivitet(eksisterende.id)).thenReturn(eksisterende)
        `when`(avtaltMedNavService.hentFHO(eksisterende.fhoId)).thenReturn(null)
        `when`(aktivitetDAO.oppdaterAktivitet(any())).thenAnswer { it.getArgument(0) }
    }

    private fun captureOppdaterAktivitet(): List<AktivitetData> {
        val captor = ArgumentCaptor.forClass(AktivitetData::class.java)
        verify(aktivitetDAO, atLeastOnce()).oppdaterAktivitet(captor.capture())
        return captor.allValues
    }

    /** Bygger en DTO med eksplisitte unike verdier for fellesfelter */
    private fun baseDtoMedUnikeVerdier(type: AktivitetTypeDTO): AktivitetDTO {
        return AktivitetDTO()
            .setType(type)
            .setTittel("TITTEL_${type.name}")
            .setBeskrivelse("BESKRIVELSE_${type.name}")
            .setLenke("https://lenke-${type.name.lowercase()}.no")
            .setFraDato(fraDato)
            .setTilDato(tilDato)
            .setStatus(AktivitetStatus.BRUKER_ER_INTERESSERT)
            .setMalid("MALID_${type.name}")
    }

    /** Asserts fellesfelter som er felles for alle opprettede aktiviteter */
    private fun assertFellesOpprettFelter(captured: AktivitetData, expectedType: AktivitetTypeData, type: AktivitetTypeDTO) {
        assertThat(captured.aktivitetType).isEqualTo(expectedType)
        assertThat(captured.aktorId).isEqualTo(aktorId)
        assertThat(captured.oppfolgingsperiodeId).isEqualTo(oppfolgingsperiodeId)
        assertThat(captured.transaksjonsType).isEqualTo(AktivitetTransaksjonsType.OPPRETTET)
        assertThat(captured.endretAv).isEqualTo(navIdent.get())
        assertThat(captured.endretAvType).isEqualTo(Innsender.NAV)
        assertThat(captured.endretDato).isNotNull()
        assertThat(captured.opprettetDato).isNotNull()
        assertThat(captured.isAutomatiskOpprettet).isFalse()
        assertThat(captured.kontorsperreEnhetId).isNull()
        assertThat(captured.status).isEqualTo(AktivitetStatus.BRUKER_ER_INTERESSERT)
        assertThat(captured.malid).isEqualTo("MALID_${type.name}")
    }

    /** Asserts muterbare felter med eksplisitt unike verdier */
    private fun assertMuterbareFelter(captured: AktivitetData, type: AktivitetTypeDTO) {
        assertThat(captured.tittel).`as`("tittel").isEqualTo("TITTEL_${type.name}")
        assertThat(captured.beskrivelse).`as`("beskrivelse").isEqualTo("BESKRIVELSE_${type.name}")
        assertThat(captured.lenke).`as`("lenke").isEqualTo("https://lenke-${type.name.lowercase()}.no")
        assertThat(captured.fraDato).`as`("fraDato").isEqualTo(fraDato)
        assertThat(captured.tilDato).`as`("tilDato").isEqualTo(tilDato)
        // Kryss-sjekk: tittel er IKKE lik beskrivelse
        assertThat(captured.tittel).isNotEqualTo(captured.beskrivelse)
        assertThat(captured.tittel).isNotEqualTo(captured.lenke)
        assertThat(captured.beskrivelse).isNotEqualTo(captured.lenke)
        assertThat(captured.fraDato).isNotEqualTo(captured.tilDato)
    }

    // =====================================================================
    // OPPRETT TESTER
    // =====================================================================
    @Nested
    inner class OpprettAktivitet {

        @Test
        fun `opprett EGEN - alle felter mappes korrekt og ikke forvekslet`() {
            setupNavBrukerContext()
            val dto = baseDtoMedUnikeVerdier(AktivitetTypeDTO.EGEN)
            dto.hensikt = "HENSIKT_UNIK"
            dto.oppfolging = "OPPFOLGING_UNIK"
            stubOpprettNyAktivitetReturnsInput()

            controller.opprettNyAktivitetPaOppfolgingsPeriode(dto, false)

            val captured = captureOpprettNyAktivitet()
            assertFellesOpprettFelter(captured, AktivitetTypeData.EGENAKTIVITET, AktivitetTypeDTO.EGEN)
            assertMuterbareFelter(captured, AktivitetTypeDTO.EGEN)
            // Type-spesifikke felter med kryss-sjekk
            assertThat(captured.egenAktivitetData).isNotNull
            assertThat(captured.egenAktivitetData.hensikt).`as`("hensikt").isEqualTo("HENSIKT_UNIK")
            assertThat(captured.egenAktivitetData.oppfolging).`as`("oppfolging").isEqualTo("OPPFOLGING_UNIK")
            // Kryss-sjekk: hensikt != oppfolging
            assertThat(captured.egenAktivitetData.hensikt).isNotEqualTo(captured.egenAktivitetData.oppfolging)
            // Hensikt skal ikke havne i noe felles-felt
            assertThat(captured.tittel).isNotEqualTo(captured.egenAktivitetData.hensikt)
            assertThat(captured.beskrivelse).isNotEqualTo(captured.egenAktivitetData.hensikt)
            // Andre type-data skal være null
            assertThat(captured.stillingsSoekAktivitetData).isNull()
            assertThat(captured.sokeAvtaleAktivitetData).isNull()
            assertThat(captured.iJobbAktivitetData).isNull()
            assertThat(captured.behandlingAktivitetData).isNull()
            assertThat(captured.moteData).isNull()
            assertThat(captured.stillingFraNavData).isNull()
            assertThat(captured.eksternAktivitetData).isNull()
        }

        @Test
        fun `opprett aktivitet med kontorSperreEnhet - enhet fra kvpService skal brukes som kontorSperreEnhet ved oppretting av aktivitet`() {
            val kontorSperreEnhetId = "0219"
            `when`(authService.getLoggedInnUser()).thenReturn(navIdent)
            `when`(authService.erEksternBruker()).thenReturn(false)
            `when`(authService.erSystemBruker()).thenReturn(false)
            `when`(userInContext.getAktorId()).thenReturn(aktorId)
            `when`(kvpService.getKontorSperreEnhet(aktorId)).thenReturn(Optional.of(EnhetId.of(kontorSperreEnhetId)))
            `when`(oppfolgingsperiodeService.hentNåværendeÅpenPeriode(aktorId)).thenReturn(
                Oppfolgingsperiode(aktorId.get(), oppfolgingsperiodeId, ZonedDateTime.now(), null)
            )
            val dto = baseDtoMedUnikeVerdier(AktivitetTypeDTO.EGEN)
            dto.hensikt = "HENSIKT_KVP"
            dto.oppfolging = "OPPFOLGING_KVP"
            stubOpprettNyAktivitetReturnsInput()

            controller.opprettNyAktivitetPaOppfolgingsPeriode(dto, false)

            val captured = captureOpprettNyAktivitet()
            assertThat(captured.kontorsperreEnhetId).isEqualTo(kontorSperreEnhetId)
            assertThat(captured.aktivitetType).isEqualTo(AktivitetTypeData.EGENAKTIVITET)
            assertThat(captured.aktorId).isEqualTo(aktorId)
        }

        @Test
        fun `opprett STILLING (jobbsoeking) - alle felter mappes korrekt og ikke forvekslet`() {
            setupNavBrukerContext()
            val dto = baseDtoMedUnikeVerdier(AktivitetTypeDTO.STILLING)
            dto.arbeidsgiver = "ARBEIDSGIVER_UNIK"
            dto.arbeidssted = "ARBEIDSSTED_UNIK"
            dto.kontaktperson = "KONTAKTPERSON_UNIK"
            dto.stillingsTittel = "STILLINGSTITTEL_UNIK"
            dto.etikett = EtikettTypeDTO.AVSLAG
            stubOpprettNyAktivitetReturnsInput()

            controller.opprettNyAktivitetPaOppfolgingsPeriode(dto, false)

            val captured = captureOpprettNyAktivitet()
            assertFellesOpprettFelter(captured, AktivitetTypeData.JOBBSOEKING, AktivitetTypeDTO.STILLING)
            assertMuterbareFelter(captured, AktivitetTypeDTO.STILLING)
            // Type-spesifikke felter
            assertThat(captured.stillingsSoekAktivitetData).isNotNull
            assertThat(captured.stillingsSoekAktivitetData.arbeidsgiver).`as`("arbeidsgiver").isEqualTo("ARBEIDSGIVER_UNIK")
            assertThat(captured.stillingsSoekAktivitetData.arbeidssted).`as`("arbeidssted").isEqualTo("ARBEIDSSTED_UNIK")
            assertThat(captured.stillingsSoekAktivitetData.kontaktPerson).`as`("kontaktPerson").isEqualTo("KONTAKTPERSON_UNIK")
            assertThat(captured.stillingsSoekAktivitetData.stillingsTittel).`as`("stillingsTittel").isEqualTo("STILLINGSTITTEL_UNIK")
            assertThat(captured.stillingsSoekAktivitetData.stillingsoekEtikett).`as`("etikett").isEqualTo(StillingsoekEtikettData.AVSLAG)
            // Kryss-sjekk: ingen forveksling mellom type-spesifikke felter
            assertThat(captured.stillingsSoekAktivitetData.arbeidsgiver).isNotEqualTo(captured.stillingsSoekAktivitetData.arbeidssted)
            assertThat(captured.stillingsSoekAktivitetData.arbeidsgiver).isNotEqualTo(captured.stillingsSoekAktivitetData.kontaktPerson)
            assertThat(captured.stillingsSoekAktivitetData.arbeidsgiver).isNotEqualTo(captured.stillingsSoekAktivitetData.stillingsTittel)
            assertThat(captured.stillingsSoekAktivitetData.arbeidssted).isNotEqualTo(captured.stillingsSoekAktivitetData.kontaktPerson)
            assertThat(captured.stillingsSoekAktivitetData.arbeidssted).isNotEqualTo(captured.stillingsSoekAktivitetData.stillingsTittel)
            // Kryss-sjekk: stillingsTittel != tittel (fellesfeltet)
            assertThat(captured.stillingsSoekAktivitetData.stillingsTittel).isNotEqualTo(captured.tittel)
            // Andre type-data skal være null
            assertThat(captured.egenAktivitetData).isNull()
            assertThat(captured.sokeAvtaleAktivitetData).isNull()
            assertThat(captured.iJobbAktivitetData).isNull()
            assertThat(captured.behandlingAktivitetData).isNull()
            assertThat(captured.moteData).isNull()
        }

        @Test
        fun `opprett SOKEAVTALE - alle felter mappes korrekt og ikke forvekslet`() {
            setupNavBrukerContext()
            val dto = baseDtoMedUnikeVerdier(AktivitetTypeDTO.SOKEAVTALE)
            dto.antallStillingerSokes = 17
            dto.antallStillingerIUken = 3
            dto.avtaleOppfolging = "AVTALEOPPFOLGING_UNIK"
            stubOpprettNyAktivitetReturnsInput()

            controller.opprettNyAktivitetPaOppfolgingsPeriode(dto, false)

            val captured = captureOpprettNyAktivitet()
            assertFellesOpprettFelter(captured, AktivitetTypeData.SOKEAVTALE, AktivitetTypeDTO.SOKEAVTALE)
            assertMuterbareFelter(captured, AktivitetTypeDTO.SOKEAVTALE)
            // Type-spesifikke felter
            assertThat(captured.sokeAvtaleAktivitetData).isNotNull
            assertThat(captured.sokeAvtaleAktivitetData.antallStillingerSokes).`as`("antallStillingerSokes").isEqualTo(17)
            assertThat(captured.sokeAvtaleAktivitetData.antallStillingerIUken).`as`("antallStillingerIUken").isEqualTo(3)
            assertThat(captured.sokeAvtaleAktivitetData.avtaleOppfolging).`as`("avtaleOppfolging").isEqualTo("AVTALEOPPFOLGING_UNIK")
            // Kryss-sjekk: antallStillingerSokes != antallStillingerIUken
            assertThat(captured.sokeAvtaleAktivitetData.antallStillingerSokes).isNotEqualTo(captured.sokeAvtaleAktivitetData.antallStillingerIUken)
            // Andre type-data skal være null
            assertThat(captured.egenAktivitetData).isNull()
            assertThat(captured.stillingsSoekAktivitetData).isNull()
            assertThat(captured.iJobbAktivitetData).isNull()
            assertThat(captured.behandlingAktivitetData).isNull()
            assertThat(captured.moteData).isNull()
        }

        @Test
        fun `opprett IJOBB - alle felter mappes korrekt og ikke forvekslet`() {
            setupNavBrukerContext()
            val dto = baseDtoMedUnikeVerdier(AktivitetTypeDTO.IJOBB)
            dto.jobbStatus = JobbStatusTypeDTO.DELTID
            dto.ansettelsesforhold = "ANSETTELSESFORHOLD_UNIK"
            dto.arbeidstid = "ARBEIDSTID_UNIK"
            stubOpprettNyAktivitetReturnsInput()

            controller.opprettNyAktivitetPaOppfolgingsPeriode(dto, false)

            val captured = captureOpprettNyAktivitet()
            assertFellesOpprettFelter(captured, AktivitetTypeData.IJOBB, AktivitetTypeDTO.IJOBB)
            assertMuterbareFelter(captured, AktivitetTypeDTO.IJOBB)
            // Type-spesifikke felter
            assertThat(captured.iJobbAktivitetData).isNotNull
            assertThat(captured.iJobbAktivitetData.jobbStatusType).`as`("jobbStatusType").isEqualTo(JobbStatusTypeData.DELTID)
            assertThat(captured.iJobbAktivitetData.ansettelsesforhold).`as`("ansettelsesforhold").isEqualTo("ANSETTELSESFORHOLD_UNIK")
            assertThat(captured.iJobbAktivitetData.arbeidstid).`as`("arbeidstid").isEqualTo("ARBEIDSTID_UNIK")
            // Kryss-sjekk
            assertThat(captured.iJobbAktivitetData.ansettelsesforhold).isNotEqualTo(captured.iJobbAktivitetData.arbeidstid)
            // Andre type-data skal være null
            assertThat(captured.egenAktivitetData).isNull()
            assertThat(captured.stillingsSoekAktivitetData).isNull()
            assertThat(captured.sokeAvtaleAktivitetData).isNull()
            assertThat(captured.behandlingAktivitetData).isNull()
            assertThat(captured.moteData).isNull()
        }

        @Test
        fun `opprett BEHANDLING - alle felter mappes korrekt og ikke forvekslet`() {
            setupNavBrukerContext()
            val dto = baseDtoMedUnikeVerdier(AktivitetTypeDTO.BEHANDLING)
            dto.behandlingType = "BEHANDLINGTYPE_UNIK"
            dto.behandlingSted = "BEHANDLINGSTED_UNIK"
            dto.effekt = "EFFEKT_UNIK"
            dto.behandlingOppfolging = "BEHANDLINGOPPFOLGING_UNIK"
            stubOpprettNyAktivitetReturnsInput()

            controller.opprettNyAktivitetPaOppfolgingsPeriode(dto, false)

            val captured = captureOpprettNyAktivitet()
            assertFellesOpprettFelter(captured, AktivitetTypeData.BEHANDLING, AktivitetTypeDTO.BEHANDLING)
            assertMuterbareFelter(captured, AktivitetTypeDTO.BEHANDLING)
            // Type-spesifikke felter
            assertThat(captured.behandlingAktivitetData).isNotNull
            assertThat(captured.behandlingAktivitetData.behandlingType).`as`("behandlingType").isEqualTo("BEHANDLINGTYPE_UNIK")
            assertThat(captured.behandlingAktivitetData.behandlingSted).`as`("behandlingSted").isEqualTo("BEHANDLINGSTED_UNIK")
            assertThat(captured.behandlingAktivitetData.effekt).`as`("effekt").isEqualTo("EFFEKT_UNIK")
            assertThat(captured.behandlingAktivitetData.behandlingOppfolging).`as`("behandlingOppfolging").isEqualTo("BEHANDLINGOPPFOLGING_UNIK")
            // Kryss-sjekk: alle fire felter er distinkte
            val behandlingFelter = listOf(
                captured.behandlingAktivitetData.behandlingType,
                captured.behandlingAktivitetData.behandlingSted,
                captured.behandlingAktivitetData.effekt,
                captured.behandlingAktivitetData.behandlingOppfolging
            )
            assertThat(behandlingFelter).doesNotHaveDuplicates()
            // Andre type-data skal være null
            assertThat(captured.egenAktivitetData).isNull()
            assertThat(captured.stillingsSoekAktivitetData).isNull()
            assertThat(captured.sokeAvtaleAktivitetData).isNull()
            assertThat(captured.iJobbAktivitetData).isNull()
            assertThat(captured.moteData).isNull()
        }

        @Test
        fun `opprett MOTE - alle felter mappes korrekt og ikke forvekslet`() {
            setupNavBrukerContext()
            val dto = baseDtoMedUnikeVerdier(AktivitetTypeDTO.MOTE)
            dto.adresse = "ADRESSE_UNIK"
            dto.forberedelser = "FORBEREDELSER_UNIK"
            dto.kanal = KanalDTO.TELEFON
            dto.referat = "REFERAT_UNIK"
            dto.isErReferatPublisert = true
            stubOpprettNyAktivitetReturnsInput()

            controller.opprettNyAktivitetPaOppfolgingsPeriode(dto, false)

            val captured = captureOpprettNyAktivitet()
            assertFellesOpprettFelter(captured, AktivitetTypeData.MOTE, AktivitetTypeDTO.MOTE)
            assertMuterbareFelter(captured, AktivitetTypeDTO.MOTE)
            // Type-spesifikke felter
            assertThat(captured.moteData).isNotNull
            assertThat(captured.moteData.adresse).`as`("adresse").isEqualTo("ADRESSE_UNIK")
            assertThat(captured.moteData.forberedelser).`as`("forberedelser").isEqualTo("FORBEREDELSER_UNIK")
            assertThat(captured.moteData.kanal).`as`("kanal").isEqualTo(KanalDTO.TELEFON)
            assertThat(captured.moteData.referat).`as`("referat").isEqualTo("REFERAT_UNIK")
            assertThat(captured.moteData.isReferatPublisert).`as`("referatPublisert").isTrue()
            // Kryss-sjekk: adresse != forberedelser != referat
            val moteFelter = listOf(captured.moteData.adresse, captured.moteData.forberedelser, captured.moteData.referat)
            assertThat(moteFelter).doesNotHaveDuplicates()
            // adresse skal ikke forveksles med felles-felter
            assertThat(captured.moteData.adresse).isNotEqualTo(captured.tittel)
            assertThat(captured.moteData.adresse).isNotEqualTo(captured.beskrivelse)
            // Andre type-data skal være null
            assertThat(captured.egenAktivitetData).isNull()
            assertThat(captured.stillingsSoekAktivitetData).isNull()
            assertThat(captured.sokeAvtaleAktivitetData).isNull()
            assertThat(captured.iJobbAktivitetData).isNull()
            assertThat(captured.behandlingAktivitetData).isNull()
        }

        @Test
        fun `opprett SAMTALEREFERAT - alle felter mappes korrekt og ikke forvekslet`() {
            setupNavBrukerContext()
            val dto = baseDtoMedUnikeVerdier(AktivitetTypeDTO.SAMTALEREFERAT)
            dto.adresse = "SAMTALE_ADRESSE_UNIK"
            dto.forberedelser = "SAMTALE_FORBEREDELSER_UNIK"
            dto.kanal = KanalDTO.OPPMOTE
            dto.referat = "SAMTALE_REFERAT_UNIK"
            dto.isErReferatPublisert = false
            stubOpprettNyAktivitetReturnsInput()

            controller.opprettNyAktivitetPaOppfolgingsPeriode(dto, false)

            val captured = captureOpprettNyAktivitet()
            assertFellesOpprettFelter(captured, AktivitetTypeData.SAMTALEREFERAT, AktivitetTypeDTO.SAMTALEREFERAT)
            assertMuterbareFelter(captured, AktivitetTypeDTO.SAMTALEREFERAT)
            // Type-spesifikke felter (samtalereferat bruker MoteData)
            assertThat(captured.moteData).isNotNull
            assertThat(captured.moteData.adresse).`as`("adresse").isEqualTo("SAMTALE_ADRESSE_UNIK")
            assertThat(captured.moteData.forberedelser).`as`("forberedelser").isEqualTo("SAMTALE_FORBEREDELSER_UNIK")
            assertThat(captured.moteData.kanal).`as`("kanal").isEqualTo(KanalDTO.OPPMOTE)
            assertThat(captured.moteData.referat).`as`("referat").isEqualTo("SAMTALE_REFERAT_UNIK")
            assertThat(captured.moteData.isReferatPublisert).`as`("referatPublisert").isFalse()
            // Kryss-sjekk
            val moteFelter = listOf(captured.moteData.adresse, captured.moteData.forberedelser, captured.moteData.referat)
            assertThat(moteFelter).doesNotHaveDuplicates()
        }

        @Test
        fun `opprett STILLING_FRA_NAV - skal gi BAD_REQUEST`() {
            `when`(authService.getLoggedInnUser()).thenReturn(navIdent)
            `when`(userInContext.getAktorId()).thenReturn(aktorId)
            `when`(kvpService.getKontorSperreEnhet(aktorId)).thenReturn(Optional.empty())
            `when`(oppfolgingsperiodeService.hentNåværendeÅpenPeriode(aktorId)).thenReturn(
                Oppfolgingsperiode(aktorId.get(), oppfolgingsperiodeId, ZonedDateTime.now(), null)
            )
            val dto = baseDtoMedUnikeVerdier(AktivitetTypeDTO.STILLING_FRA_NAV)

            val exception = assertThrows<ResponseStatusException> {
                controller.opprettNyAktivitetPaOppfolgingsPeriode(dto, false)
            }
            assertThat(exception.statusCode.value()).isEqualTo(400)
            verify(aktivitetDAO, never()).opprettNyAktivitet(any())
        }

        @Test
        fun `opprett aktivitet med automatisk flagg settes korrekt`() {
            setupNavBrukerContext()
            val dto = baseDtoMedUnikeVerdier(AktivitetTypeDTO.EGEN)
            dto.hensikt = "hensikt"
            dto.oppfolging = "oppfolging"
            stubOpprettNyAktivitetReturnsInput()

            controller.opprettNyAktivitetPaOppfolgingsPeriode(dto, true)

            val captured = captureOpprettNyAktivitet()
            assertThat(captured.isAutomatiskOpprettet).isTrue()
        }

        @Test
        fun `opprettet aktivitet returnerer korrekt DTO tilbake med riktige felter`() {
            setupNavBrukerContext()
            val dto = baseDtoMedUnikeVerdier(AktivitetTypeDTO.EGEN)
            dto.hensikt = "RETUR_HENSIKT"
            dto.oppfolging = "RETUR_OPPFOLGING"

            `when`(aktivitetDAO.opprettNyAktivitet(any())).thenAnswer { invocation ->
                (invocation.getArgument(0) as AktivitetData).toBuilder().id(99L).versjon(1L).build()
            }

            val result = controller.opprettNyAktivitetPaOppfolgingsPeriode(dto, false)

            assertThat(result.id).isEqualTo("99")
            assertThat(result.versjon).isEqualTo("1")
            assertThat(result.type).isEqualTo(AktivitetTypeDTO.EGEN)
            assertThat(result.tittel).isEqualTo("TITTEL_EGEN")
            assertThat(result.beskrivelse).isEqualTo("BESKRIVELSE_EGEN")
            assertThat(result.lenke).isEqualTo("https://lenke-egen.no")
            assertThat(result.hensikt).isEqualTo("RETUR_HENSIKT")
            assertThat(result.oppfolging).isEqualTo("RETUR_OPPFOLGING")
            // Kryss-sjekk: felter er ikke forvekslet i retur-DTO
            assertThat(result.tittel).isNotEqualTo(result.beskrivelse)
            assertThat(result.hensikt).isNotEqualTo(result.oppfolging)
        }
    }

    // =====================================================================
    // OPPDATER TESTER - NAV (intern bruker)
    // =====================================================================
    @Nested
    inner class OppdaterSomNav {

        @Test
        fun `oppdater aktivitet opprettet før kontorsperre - kvpService getKontorSperreEnhet kalles ikke ved oppdatering`() {
            /* Aktiviteter opprettet før brukere er under KVP skal ikke få kontorsperre selvom de oppdateres når bruker er under kvp
            * Det går ikke an å sette opp unødvendige mocks i mockito og i dette tilfellet er å mocke at bruker er under KVP en
            * unødvendig mock siden den aldri kalles  */
            setupNavBrukerContextForOppdater()
            val eksisterende = AktivitetDataTestBuilder.nyEgenaktivitet().toBuilder()
                .id(99L)
                .versjon(3L)
                .avtalt(false)
                .status(AktivitetStatus.PLANLAGT)
                .historiskDato(null)
                .kontorsperreEnhetId("0219")
                .tittel("ORIGINAL_TITTEL").beskrivelse("ORIGINAL_BESKRIVELSE")
                .egenAktivitetData(EgenAktivitetData.builder().hensikt("ORIGINAL_HENSIKT").oppfolging("ORIGINAL_OPPFOLGING").build())
                .build()
            stubHentOgOppdater(eksisterende)

            val dto = AktivitetDTOMapper.mapTilAktivitetDTO(eksisterende, false)
            dto.tittel = "NY_TITTEL"

            controller.oppdaterAktivitet(dto)

            val captured = captureOppdaterAktivitet().last()
            assertThat(captured.kontorsperreEnhetId).isEqualTo("0219")
            assertThat(captured.tittel).isEqualTo("NY_TITTEL")
            verify(kvpService, never()).getKontorSperreEnhet(any())
        }

        @Test
        fun `oppdater EGEN - ikke avtalt - endrede felter oppdateres, uendrede beholdes`() {
            setupNavBrukerContextForOppdater()
            val eksisterende = AktivitetDataTestBuilder.nyEgenaktivitet().toBuilder()
                .id(42L).versjon(5L).avtalt(false).status(AktivitetStatus.PLANLAGT).historiskDato(null)
                .tittel("ORIGINAL_TITTEL").beskrivelse("ORIGINAL_BESKRIVELSE").lenke("ORIGINAL_LENKE")
                .egenAktivitetData(EgenAktivitetData.builder().hensikt("ORIGINAL_HENSIKT").oppfolging("ORIGINAL_OPPFOLGING").build())
                .build()
            stubHentOgOppdater(eksisterende)

            val dto = AktivitetDTOMapper.mapTilAktivitetDTO(eksisterende, false)
            dto.tittel = "NY_TITTEL"
            dto.beskrivelse = "NY_BESKRIVELSE"
            dto.hensikt = "NY_HENSIKT"
            dto.oppfolging = "NY_OPPFOLGING"
            dto.lenke = "NY_LENKE"

            controller.oppdaterAktivitet(dto)

            val captured = captureOppdaterAktivitet().last()
            assertThat(captured.transaksjonsType).isEqualTo(AktivitetTransaksjonsType.DETALJER_ENDRET)
            assertThat(captured.endretAv).isEqualTo(navIdent.get())
            assertThat(captured.endretAvType).isEqualTo(Innsender.NAV)
            assertThat(captured.endretDato).isNotNull()
            // Endrede felter
            assertThat(captured.tittel).`as`("tittel").isEqualTo("NY_TITTEL")
            assertThat(captured.beskrivelse).`as`("beskrivelse").isEqualTo("NY_BESKRIVELSE")
            assertThat(captured.lenke).`as`("lenke").isEqualTo("NY_LENKE")
            assertThat(captured.egenAktivitetData.hensikt).`as`("hensikt").isEqualTo("NY_HENSIKT")
            assertThat(captured.egenAktivitetData.oppfolging).`as`("oppfolging").isEqualTo("NY_OPPFOLGING")
            // Kryss-sjekk: ingen forveksling
            assertThat(captured.tittel).isNotEqualTo(captured.beskrivelse)
            assertThat(captured.tittel).isNotEqualTo(captured.lenke)
            assertThat(captured.egenAktivitetData.hensikt).isNotEqualTo(captured.egenAktivitetData.oppfolging)
            assertThat(captured.egenAktivitetData.hensikt).isNotEqualTo(captured.tittel)
            // Uendrede felter beholder opprinnelig verdi
            assertThat(captured.id).isEqualTo(42L)
            assertThat(captured.aktivitetType).isEqualTo(AktivitetTypeData.EGENAKTIVITET)
            assertThat(captured.fraDato).isEqualTo(eksisterende.fraDato)
            assertThat(captured.tilDato).isEqualTo(eksisterende.tilDato)
            assertThat(captured.status).isEqualTo(eksisterende.status)
        }

        @Test
        fun `oppdater EGEN - avtalt - kun frist endres, andre felter beholdes`() {
            setupNavBrukerContextForOppdater()
            val eksisterende = AktivitetDataTestBuilder.nyEgenaktivitet().toBuilder()
                .id(42L).versjon(5L).avtalt(true).status(AktivitetStatus.PLANLAGT).historiskDato(null)
                .tittel("UENDRET_TITTEL").beskrivelse("UENDRET_BESKRIVELSE")
                .build()
            stubHentOgOppdater(eksisterende)

            val nyTilDato = Date(1900000000000L)
            val dto = AktivitetDTOMapper.mapTilAktivitetDTO(eksisterende, false)
            dto.tilDato = nyTilDato
            dto.tittel = "IGNORERT_TITTEL"
            dto.beskrivelse = "IGNORERT_BESKRIVELSE"

            controller.oppdaterAktivitet(dto)

            val captured = captureOppdaterAktivitet().last()
            assertThat(captured.transaksjonsType).isEqualTo(AktivitetTransaksjonsType.AVTALT_DATO_ENDRET)
            assertThat(captured.tilDato).`as`("tilDato").isEqualTo(nyTilDato)
            // Tittel og beskrivelse skal IKKE være endret
            assertThat(captured.tittel).`as`("tittel uendret").isEqualTo("UENDRET_TITTEL")
            assertThat(captured.beskrivelse).`as`("beskrivelse uendret").isEqualTo("UENDRET_BESKRIVELSE")
            assertThat(captured.fraDato).`as`("fraDato uendret").isEqualTo(eksisterende.fraDato)
            assertThat(captured.endretAv).isEqualTo(navIdent.get())
            assertThat(captured.endretAvType).isEqualTo(Innsender.NAV)
        }

        @Test
        fun `oppdater STILLING (jobbsoeking) - ikke avtalt - typespesifikke data korrekt mappet`() {
            setupNavBrukerContextForOppdater()
            val eksisterende = AktivitetDataTestBuilder.nyttStillingssok().toBuilder()
                .id(43L).versjon(10L).avtalt(false).status(AktivitetStatus.PLANLAGT).historiskDato(null)
                .stillingsSoekAktivitetData(StillingsoekAktivitetData.builder()
                    .arbeidsgiver("ORIG_ARBEIDSGIVER").arbeidssted("ORIG_ARBEIDSSTED")
                    .kontaktPerson("ORIG_KONTAKT").stillingsTittel("ORIG_STILLINGSTITTEL")
                    .stillingsoekEtikett(StillingsoekEtikettData.SOKNAD_SENDT).build())
                .build()
            stubHentOgOppdater(eksisterende)

            val dto = AktivitetDTOMapper.mapTilAktivitetDTO(eksisterende, false)
            dto.arbeidsgiver = "NY_ARBEIDSGIVER"
            dto.stillingsTittel = "NY_STILLINGSTITTEL"
            dto.arbeidssted = "NY_ARBEIDSSTED"
            dto.kontaktperson = "NY_KONTAKTPERSON"

            controller.oppdaterAktivitet(dto)

            val captured = captureOppdaterAktivitet().last()
            assertThat(captured.transaksjonsType).isEqualTo(AktivitetTransaksjonsType.DETALJER_ENDRET)
            assertThat(captured.stillingsSoekAktivitetData.arbeidsgiver).`as`("arbeidsgiver").isEqualTo("NY_ARBEIDSGIVER")
            assertThat(captured.stillingsSoekAktivitetData.stillingsTittel).`as`("stillingsTittel").isEqualTo("NY_STILLINGSTITTEL")
            assertThat(captured.stillingsSoekAktivitetData.arbeidssted).`as`("arbeidssted").isEqualTo("NY_ARBEIDSSTED")
            assertThat(captured.stillingsSoekAktivitetData.kontaktPerson).`as`("kontaktPerson").isEqualTo("NY_KONTAKTPERSON")
            // Kryss-sjekk: alle felt-verdier er distinkte
            val felter = listOf(
                captured.stillingsSoekAktivitetData.arbeidsgiver,
                captured.stillingsSoekAktivitetData.stillingsTittel,
                captured.stillingsSoekAktivitetData.arbeidssted,
                captured.stillingsSoekAktivitetData.kontaktPerson
            )
            assertThat(felter).doesNotHaveDuplicates()
            // stillingsTittel er ikke forvekslet med felles tittel
            assertThat(captured.stillingsSoekAktivitetData.stillingsTittel).isNotEqualTo(captured.tittel)
        }

        @Test
        fun `oppdater SOKEAVTALE - ikke avtalt - typespesifikke data korrekt mappet`() {
            setupNavBrukerContextForOppdater()
            val eksisterende = AktivitetDataTestBuilder.nySokeAvtaleAktivitet().toBuilder()
                .id(44L).versjon(3L).avtalt(false).status(AktivitetStatus.PLANLAGT).historiskDato(null).build()
            stubHentOgOppdater(eksisterende)

            val dto = AktivitetDTOMapper.mapTilAktivitetDTO(eksisterende, false)
            dto.antallStillingerSokes = 23
            dto.antallStillingerIUken = 7
            dto.avtaleOppfolging = "NY_AVTALEOPPFOLGING"

            controller.oppdaterAktivitet(dto)

            val captured = captureOppdaterAktivitet().last()
            assertThat(captured.transaksjonsType).isEqualTo(AktivitetTransaksjonsType.DETALJER_ENDRET)
            assertThat(captured.sokeAvtaleAktivitetData.antallStillingerSokes).`as`("antallStillingerSokes").isEqualTo(23)
            assertThat(captured.sokeAvtaleAktivitetData.antallStillingerIUken).`as`("antallStillingerIUken").isEqualTo(7)
            assertThat(captured.sokeAvtaleAktivitetData.avtaleOppfolging).`as`("avtaleOppfolging").isEqualTo("NY_AVTALEOPPFOLGING")
            // Kryss-sjekk: de to tallene er ulike
            assertThat(captured.sokeAvtaleAktivitetData.antallStillingerSokes).isNotEqualTo(captured.sokeAvtaleAktivitetData.antallStillingerIUken)
        }

        @Test
        fun `oppdater IJOBB - ikke avtalt - typespesifikke data korrekt mappet`() {
            setupNavBrukerContextForOppdater()
            val eksisterende = AktivitetDataTestBuilder.nyIJobbAktivitet().toBuilder()
                .id(45L).versjon(2L).avtalt(false).status(AktivitetStatus.PLANLAGT).historiskDato(null).build()
            stubHentOgOppdater(eksisterende)

            val dto = AktivitetDTOMapper.mapTilAktivitetDTO(eksisterende, false)
            dto.ansettelsesforhold = "NY_ANSETTELSESFORHOLD"
            dto.arbeidstid = "NY_ARBEIDSTID"

            controller.oppdaterAktivitet(dto)

            val captured = captureOppdaterAktivitet().last()
            assertThat(captured.transaksjonsType).isEqualTo(AktivitetTransaksjonsType.DETALJER_ENDRET)
            assertThat(captured.iJobbAktivitetData.ansettelsesforhold).`as`("ansettelsesforhold").isEqualTo("NY_ANSETTELSESFORHOLD")
            assertThat(captured.iJobbAktivitetData.arbeidstid).`as`("arbeidstid").isEqualTo("NY_ARBEIDSTID")
            // Kryss-sjekk
            assertThat(captured.iJobbAktivitetData.ansettelsesforhold).isNotEqualTo(captured.iJobbAktivitetData.arbeidstid)
            // jobbStatusType er uendret
            assertThat(captured.iJobbAktivitetData.jobbStatusType).isEqualTo(eksisterende.iJobbAktivitetData.jobbStatusType)
        }

        @Test
        fun `oppdater BEHANDLING - ikke avtalt - typespesifikke data korrekt mappet`() {
            setupNavBrukerContextForOppdater()
            val eksisterende = AktivitetDataTestBuilder.nyBehandlingAktivitet().toBuilder()
                .id(46L).versjon(2L).avtalt(false).status(AktivitetStatus.PLANLAGT).historiskDato(null)
                .behandlingAktivitetData(BehandlingAktivitetData.builder()
                    .behandlingType("ORIG_TYPE").behandlingSted("ORIG_STED")
                    .effekt("ORIG_EFFEKT").behandlingOppfolging("ORIG_OPPFOLGING").build())
                .build()
            stubHentOgOppdater(eksisterende)

            val dto = AktivitetDTOMapper.mapTilAktivitetDTO(eksisterende, false)
            dto.behandlingType = "NY_BEHANDLINGTYPE"
            dto.behandlingSted = "NY_BEHANDLINGSTED"
            dto.effekt = "NY_EFFEKT"
            dto.behandlingOppfolging = "NY_BEHANDLINGOPPFOLGING"

            controller.oppdaterAktivitet(dto)

            val captured = captureOppdaterAktivitet().last()
            assertThat(captured.transaksjonsType).isEqualTo(AktivitetTransaksjonsType.DETALJER_ENDRET)
            assertThat(captured.behandlingAktivitetData.behandlingType).`as`("behandlingType").isEqualTo("NY_BEHANDLINGTYPE")
            assertThat(captured.behandlingAktivitetData.behandlingSted).`as`("behandlingSted").isEqualTo("NY_BEHANDLINGSTED")
            assertThat(captured.behandlingAktivitetData.effekt).`as`("effekt").isEqualTo("NY_EFFEKT")
            assertThat(captured.behandlingAktivitetData.behandlingOppfolging).`as`("behandlingOppfolging").isEqualTo("NY_BEHANDLINGOPPFOLGING")
            // Kryss-sjekk: alle fire felter er distinkte
            val behandlingFelter = listOf(
                captured.behandlingAktivitetData.behandlingType,
                captured.behandlingAktivitetData.behandlingSted,
                captured.behandlingAktivitetData.effekt,
                captured.behandlingAktivitetData.behandlingOppfolging
            )
            assertThat(behandlingFelter).doesNotHaveDuplicates()
        }

        @Test
        fun `oppdater MOTE - ikke avtalt - alle motefelter korrekt mappet, uendrede beholdes`() {
            setupNavBrukerContextForOppdater()
            val eksisterende = AktivitetDataTestBuilder.nyAktivitet()
                .aktivitetType(AktivitetTypeData.MOTE)
                .moteData(MoteData.builder().adresse("ORIG_ADRESSE").forberedelser("ORIG_FORBEREDELSER")
                    .kanal(KanalDTO.TELEFON).referat("ORIG_REFERAT").referatPublisert(false).build())
                .id(47L).versjon(2L).avtalt(false).status(AktivitetStatus.PLANLAGT).historiskDato(null)
                .tittel("ORIG_MOTETITTEL")
                .build()
            stubHentOgOppdater(eksisterende)

            val dto = AktivitetDTOMapper.mapTilAktivitetDTO(eksisterende, false)
            dto.adresse = "NY_ADRESSE"
            dto.forberedelser = "NY_FORBEREDELSER"
            dto.tittel = "NY_MOTETITTEL"

            controller.oppdaterAktivitet(dto)

            val captured = captureOppdaterAktivitet().last()
            assertThat(captured.transaksjonsType).isEqualTo(AktivitetTransaksjonsType.DETALJER_ENDRET)
            assertThat(captured.tittel).`as`("tittel").isEqualTo("NY_MOTETITTEL")
            assertThat(captured.moteData.adresse).`as`("adresse").isEqualTo("NY_ADRESSE")
            assertThat(captured.moteData.forberedelser).`as`("forberedelser").isEqualTo("NY_FORBEREDELSER")
            // Kryss-sjekk: tittel != adresse != forberedelser
            assertThat(captured.tittel).isNotEqualTo(captured.moteData.adresse)
            assertThat(captured.tittel).isNotEqualTo(captured.moteData.forberedelser)
            assertThat(captured.moteData.adresse).isNotEqualTo(captured.moteData.forberedelser)
            // Uendrede motefelter beholder opprinnelig verdi
            assertThat(captured.moteData.kanal).`as`("kanal uendret").isEqualTo(KanalDTO.TELEFON)
        }

        @Test
        fun `oppdater MOTE - avtalt - tid og sted endret, detaljer uendret`() {
            setupNavBrukerContextForOppdater()
            val eksisterendeMoteData = MoteData.builder()
                .adresse("ORIG_ADRESSE").kanal(KanalDTO.TELEFON).forberedelser("ORIG_FORBEREDELSER").build()
            val eksisterende = AktivitetDataTestBuilder.nyAktivitet()
                .aktivitetType(AktivitetTypeData.MOTE).moteData(eksisterendeMoteData)
                .id(48L).versjon(3L).avtalt(true).status(AktivitetStatus.PLANLAGT).historiskDato(null)
                .tittel("UENDRET_TITTEL").beskrivelse("UENDRET_BESKRIVELSE")
                .build()
            stubHentOgOppdater(eksisterende)

            val dto = AktivitetDTOMapper.mapTilAktivitetDTO(eksisterende, false)
            dto.adresse = "NY_ADRESSE"
            val nyFraDato = Date(1750000000000L)
            dto.fraDato = nyFraDato

            controller.oppdaterAktivitet(dto)

            val allCaptures = captureOppdaterAktivitet()
            val tidOgSted = allCaptures.first()
            assertThat(tidOgSted.transaksjonsType).isEqualTo(AktivitetTransaksjonsType.MOTE_TID_OG_STED_ENDRET)
            assertThat(tidOgSted.moteData.adresse).`as`("adresse endret").isEqualTo("NY_ADRESSE")
            assertThat(tidOgSted.fraDato).`as`("fraDato endret").isEqualTo(nyFraDato)
            assertThat(tidOgSted.endretAv).isEqualTo(navIdent.get())
            assertThat(tidOgSted.endretAvType).isEqualTo(Innsender.NAV)
            // Kanal beholdes
            assertThat(tidOgSted.moteData.kanal).`as`("kanal uendret").isEqualTo(KanalDTO.TELEFON)
            // Tittel og beskrivelse er IKKE endret i denne operasjonen
            assertThat(tidOgSted.tittel).`as`("tittel uendret").isEqualTo("UENDRET_TITTEL")
            assertThat(tidOgSted.beskrivelse).`as`("beskrivelse uendret").isEqualTo("UENDRET_BESKRIVELSE")
        }

        @Test
        fun `oppdater MOTE - avtalt - detaljer endret, tid-sted uendret`() {
            setupNavBrukerContextForOppdater()
            val eksisterendeMoteData = MoteData.builder()
                .adresse("UENDRET_ADRESSE").kanal(null).forberedelser("ORIG_FORBEREDELSER").build()
            val eksisterende = AktivitetDataTestBuilder.nyAktivitet()
                .aktivitetType(AktivitetTypeData.MOTE).moteData(eksisterendeMoteData)
                .id(49L).versjon(3L).avtalt(true).status(AktivitetStatus.PLANLAGT).historiskDato(null)
                .tittel("ORIG_TITTEL")
                .build()
            stubHentOgOppdater(eksisterende)

            val dto = AktivitetDTOMapper.mapTilAktivitetDTO(eksisterende, false)
            dto.tittel = "NY_MOTETITTEL"
            dto.forberedelser = "NY_FORBEREDELSER"

            controller.oppdaterAktivitet(dto)

            val allCaptures = captureOppdaterAktivitet()
            val detaljer = allCaptures.last()
            assertThat(detaljer.transaksjonsType).isEqualTo(AktivitetTransaksjonsType.DETALJER_ENDRET)
            assertThat(detaljer.tittel).`as`("tittel endret").isEqualTo("NY_MOTETITTEL")
            assertThat(detaljer.moteData.forberedelser).`as`("forberedelser endret").isEqualTo("NY_FORBEREDELSER")
            // Kryss-sjekk
            assertThat(detaljer.tittel).isNotEqualTo(detaljer.moteData.forberedelser)
            assertThat(detaljer.moteData.adresse).`as`("adresse uendret").isEqualTo("UENDRET_ADRESSE")
        }

        @Test
        fun `oppdater MOTE - avtalt - bade tid-sted og detaljer endret genererer to DAO kall`() {
            setupNavBrukerContextForOppdater()
            val eksisterendeMoteData = MoteData.builder()
                .adresse("ORIG_ADRESSE").kanal(null).forberedelser("ORIG_FORBEREDELSER").build()
            val eksisterende = AktivitetDataTestBuilder.nyAktivitet()
                .aktivitetType(AktivitetTypeData.MOTE).moteData(eksisterendeMoteData)
                .id(50L).versjon(3L).avtalt(true).status(AktivitetStatus.PLANLAGT).historiskDato(null)
                .tittel("ORIG_TITTEL")
                .build()
            stubHentOgOppdater(eksisterende)

            val dto = AktivitetDTOMapper.mapTilAktivitetDTO(eksisterende, false)
            dto.adresse = "NY_ADRESSE"
            dto.fraDato = Date(1750000000000L)
            dto.tittel = "NY_MOTETITTEL"
            dto.forberedelser = "NY_FORBEREDELSER"

            controller.oppdaterAktivitet(dto)

            val allCaptures = captureOppdaterAktivitet()
            assertThat(allCaptures).hasSizeGreaterThanOrEqualTo(2)
            assertThat(allCaptures[0].transaksjonsType).isEqualTo(AktivitetTransaksjonsType.MOTE_TID_OG_STED_ENDRET)
            assertThat(allCaptures[0].moteData.adresse).isEqualTo("NY_ADRESSE")
            assertThat(allCaptures[1].transaksjonsType).isEqualTo(AktivitetTransaksjonsType.DETALJER_ENDRET)
            assertThat(allCaptures[1].tittel).isEqualTo("NY_MOTETITTEL")
            assertThat(allCaptures[1].moteData.forberedelser).isEqualTo("NY_FORBEREDELSER")
        }

        @Test
        fun `oppdater STILLING_FRA_NAV - skal gi BAD_REQUEST`() {
            `when`(authService.getLoggedInnUser()).thenReturn(navIdent)
            val eksisterende = AktivitetDataTestBuilder.nyStillingFraNavMedCVKanDeles().toBuilder()
                .id(60L).versjon(2L).avtalt(false).status(AktivitetStatus.PLANLAGT).historiskDato(null).build()
            `when`(aktivitetDAO.hentAktivitet(eksisterende.id)).thenReturn(eksisterende)

            val dto = AktivitetDTOMapper.mapTilAktivitetDTO(eksisterende, false)

            val exception = assertThrows<ResponseStatusException> {
                controller.oppdaterAktivitet(dto)
            }
            assertThat(exception.statusCode.value()).isEqualTo(400)
            verify(aktivitetDAO, never()).oppdaterAktivitet(any())
        }
    }

    // =====================================================================
    // OPPDATER TESTER - EKSTERN BRUKER
    // =====================================================================
    @Nested
    inner class OppdaterSomEksternBruker {

        @Test
        fun `oppdater BEHANDLING - ikke avtalt - alle felter korrekt mappet som ekstern`() {
            setupEksternBrukerContextForOppdater()
            val eksisterende = AktivitetDataTestBuilder.nyBehandlingAktivitet().toBuilder()
                .id(50L).versjon(2L).avtalt(false).status(AktivitetStatus.PLANLAGT).historiskDato(null)
                .behandlingAktivitetData(BehandlingAktivitetData.builder()
                    .behandlingType("ORIG_TYPE").behandlingSted("ORIG_STED")
                    .effekt("ORIG_EFFEKT").behandlingOppfolging("ORIG_OPPFOLGING").build())
                .build()
            stubHentOgOppdater(eksisterende)

            val dto = AktivitetDTOMapper.mapTilAktivitetDTO(eksisterende, true)
            dto.behandlingSted = "EKSTERN_STED"
            dto.behandlingType = "EKSTERN_TYPE"
            dto.effekt = "EKSTERN_EFFEKT"
            dto.behandlingOppfolging = "EKSTERN_OPPFOLGING"

            controller.oppdaterAktivitet(dto)

            val captured = captureOppdaterAktivitet().last()
            assertThat(captured.endretAvType).isEqualTo(Innsender.BRUKER)
            assertThat(captured.transaksjonsType).isEqualTo(AktivitetTransaksjonsType.DETALJER_ENDRET)
            assertThat(captured.behandlingAktivitetData.behandlingSted).`as`("behandlingSted").isEqualTo("EKSTERN_STED")
            assertThat(captured.behandlingAktivitetData.behandlingType).`as`("behandlingType").isEqualTo("EKSTERN_TYPE")
            assertThat(captured.behandlingAktivitetData.effekt).`as`("effekt").isEqualTo("EKSTERN_EFFEKT")
            assertThat(captured.behandlingAktivitetData.behandlingOppfolging).`as`("behandlingOppfolging").isEqualTo("EKSTERN_OPPFOLGING")
            // Kryss-sjekk
            val felter = listOf(
                captured.behandlingAktivitetData.behandlingSted,
                captured.behandlingAktivitetData.behandlingType,
                captured.behandlingAktivitetData.effekt,
                captured.behandlingAktivitetData.behandlingOppfolging
            )
            assertThat(felter).doesNotHaveDuplicates()
        }

        @Test
        fun `oppdater BEHANDLING - avtalt - kun frist endres, andre felter beholdes`() {
            setupEksternBrukerContextForOppdater()
            val eksisterende = AktivitetDataTestBuilder.nyBehandlingAktivitet().toBuilder()
                .id(51L).versjon(2L).avtalt(true).status(AktivitetStatus.PLANLAGT).historiskDato(null)
                .tittel("UENDRET_TITTEL")
                .behandlingAktivitetData(BehandlingAktivitetData.builder()
                    .behandlingType("UENDRET_TYPE").behandlingSted("UENDRET_STED")
                    .effekt("UENDRET_EFFEKT").behandlingOppfolging("UENDRET_OPPFOLGING").build())
                .build()
            stubHentOgOppdater(eksisterende)

            val nyTilDato = Date(1900000000000L)
            val dto = AktivitetDTOMapper.mapTilAktivitetDTO(eksisterende, true)
            dto.tilDato = nyTilDato

            controller.oppdaterAktivitet(dto)

            val captured = captureOppdaterAktivitet().last()
            assertThat(captured.transaksjonsType).isEqualTo(AktivitetTransaksjonsType.AVTALT_DATO_ENDRET)
            assertThat(captured.tilDato).isEqualTo(nyTilDato)
            assertThat(captured.endretAvType).isEqualTo(Innsender.BRUKER)
            // Alle andre felter beholdes
            assertThat(captured.tittel).`as`("tittel uendret").isEqualTo("UENDRET_TITTEL")
            assertThat(captured.behandlingAktivitetData.behandlingType).`as`("behandlingType uendret").isEqualTo("UENDRET_TYPE")
            assertThat(captured.behandlingAktivitetData.behandlingSted).`as`("behandlingSted uendret").isEqualTo("UENDRET_STED")
        }

        @Test
        fun `oppdater EGEN som ekstern - ikke avtalt - korrekt mapping`() {
            setupEksternBrukerContextForOppdater()
            val eksisterende = AktivitetDataTestBuilder.nyEgenaktivitet().toBuilder()
                .id(52L).versjon(2L).avtalt(false).status(AktivitetStatus.PLANLAGT).historiskDato(null)
                .egenAktivitetData(EgenAktivitetData.builder().hensikt("ORIG_HENSIKT").oppfolging("ORIG_OPPFOLGING").build())
                .build()
            stubHentOgOppdater(eksisterende)

            val dto = AktivitetDTOMapper.mapTilAktivitetDTO(eksisterende, true)
            dto.hensikt = "EKSTERN_HENSIKT"
            dto.oppfolging = "EKSTERN_OPPFOLGING"

            controller.oppdaterAktivitet(dto)

            val captured = captureOppdaterAktivitet().last()
            assertThat(captured.endretAvType).isEqualTo(Innsender.BRUKER)
            assertThat(captured.egenAktivitetData.hensikt).`as`("hensikt").isEqualTo("EKSTERN_HENSIKT")
            assertThat(captured.egenAktivitetData.oppfolging).`as`("oppfolging").isEqualTo("EKSTERN_OPPFOLGING")
            // Kryss-sjekk
            assertThat(captured.egenAktivitetData.hensikt).isNotEqualTo(captured.egenAktivitetData.oppfolging)
        }

        @Test
        fun `oppdater STILLING som ekstern - ikke avtalt - korrekt mapping`() {
            setupEksternBrukerContextForOppdater()
            val eksisterende = AktivitetDataTestBuilder.nyttStillingssok().toBuilder()
                .id(53L).versjon(2L).avtalt(false).status(AktivitetStatus.PLANLAGT).historiskDato(null)
                .stillingsSoekAktivitetData(StillingsoekAktivitetData.builder()
                    .arbeidsgiver("ORIG_AG").arbeidssted("ORIG_AS").kontaktPerson("ORIG_KP").stillingsTittel("ORIG_ST")
                    .stillingsoekEtikett(StillingsoekEtikettData.SOKNAD_SENDT).build())
                .build()
            stubHentOgOppdater(eksisterende)

            val dto = AktivitetDTOMapper.mapTilAktivitetDTO(eksisterende, true)
            dto.arbeidsgiver = "EKSTERN_ARBEIDSGIVER"
            dto.stillingsTittel = "EKSTERN_STILLINGSTITTEL"

            controller.oppdaterAktivitet(dto)

            val captured = captureOppdaterAktivitet().last()
            assertThat(captured.endretAvType).isEqualTo(Innsender.BRUKER)
            assertThat(captured.stillingsSoekAktivitetData.arbeidsgiver).`as`("arbeidsgiver").isEqualTo("EKSTERN_ARBEIDSGIVER")
            assertThat(captured.stillingsSoekAktivitetData.stillingsTittel).`as`("stillingsTittel").isEqualTo("EKSTERN_STILLINGSTITTEL")
            // Kryss-sjekk
            assertThat(captured.stillingsSoekAktivitetData.arbeidsgiver).isNotEqualTo(captured.stillingsSoekAktivitetData.stillingsTittel)
        }

        @Test
        fun `oppdater IJOBB som ekstern - ikke avtalt - korrekt mapping`() {
            setupEksternBrukerContextForOppdater()
            val eksisterende = AktivitetDataTestBuilder.nyIJobbAktivitet().toBuilder()
                .id(54L).versjon(2L).avtalt(false).status(AktivitetStatus.PLANLAGT).historiskDato(null).build()
            stubHentOgOppdater(eksisterende)

            val dto = AktivitetDTOMapper.mapTilAktivitetDTO(eksisterende, true)
            dto.ansettelsesforhold = "EKSTERN_ANSETTELSE"
            dto.arbeidstid = "EKSTERN_ARBEIDSTID"

            controller.oppdaterAktivitet(dto)

            val captured = captureOppdaterAktivitet().last()
            assertThat(captured.endretAvType).isEqualTo(Innsender.BRUKER)
            assertThat(captured.iJobbAktivitetData.ansettelsesforhold).`as`("ansettelsesforhold").isEqualTo("EKSTERN_ANSETTELSE")
            assertThat(captured.iJobbAktivitetData.arbeidstid).`as`("arbeidstid").isEqualTo("EKSTERN_ARBEIDSTID")
            assertThat(captured.iJobbAktivitetData.ansettelsesforhold).isNotEqualTo(captured.iJobbAktivitetData.arbeidstid)
        }

        @Test
        fun `oppdater SOKEAVTALE som ekstern - ikke avtalt - korrekt mapping`() {
            setupEksternBrukerContextForOppdater()
            val eksisterende = AktivitetDataTestBuilder.nySokeAvtaleAktivitet().toBuilder()
                .id(55L).versjon(2L).avtalt(false).status(AktivitetStatus.PLANLAGT).historiskDato(null).build()
            stubHentOgOppdater(eksisterende)

            val dto = AktivitetDTOMapper.mapTilAktivitetDTO(eksisterende, true)
            dto.antallStillingerSokes = 31
            dto.antallStillingerIUken = 4
            dto.avtaleOppfolging = "EKSTERN_OPPFOLGING"

            controller.oppdaterAktivitet(dto)

            val captured = captureOppdaterAktivitet().last()
            assertThat(captured.endretAvType).isEqualTo(Innsender.BRUKER)
            assertThat(captured.sokeAvtaleAktivitetData.antallStillingerSokes).`as`("antallStillingerSokes").isEqualTo(31)
            assertThat(captured.sokeAvtaleAktivitetData.antallStillingerIUken).`as`("antallStillingerIUken").isEqualTo(4)
            assertThat(captured.sokeAvtaleAktivitetData.avtaleOppfolging).`as`("avtaleOppfolging").isEqualTo("EKSTERN_OPPFOLGING")
            assertThat(captured.sokeAvtaleAktivitetData.antallStillingerSokes).isNotEqualTo(captured.sokeAvtaleAktivitetData.antallStillingerIUken)
        }

        @Test
        fun `oppdater MOTE som ekstern - avvises`() {
            setupEksternBrukerContextForOppdater()
            val eksisterende = AktivitetDataTestBuilder.nyMoteAktivitet().toBuilder()
                .id(56L).versjon(2L).avtalt(false).status(AktivitetStatus.PLANLAGT).historiskDato(null).build()
            `when`(aktivitetDAO.hentAktivitet(eksisterende.id)).thenReturn(eksisterende)

            val dto = AktivitetDTOMapper.mapTilAktivitetDTO(eksisterende, true)

            val exception = assertThrows<ResponseStatusException> {
                controller.oppdaterAktivitet(dto)
            }
            assertThat(exception.statusCode.value()).isEqualTo(400)
            verify(aktivitetDAO, never()).oppdaterAktivitet(any())
        }

        @Test
        fun `oppdater SAMTALEREFERAT som ekstern - avvises`() {
            setupEksternBrukerContextForOppdater()
            val eksisterende = AktivitetDataTestBuilder.nySamtaleReferat().toBuilder()
                .id(57L).versjon(2L).avtalt(false).status(AktivitetStatus.PLANLAGT).historiskDato(null).build()
            `when`(aktivitetDAO.hentAktivitet(eksisterende.id)).thenReturn(eksisterende)
            val dto = AktivitetDTOMapper.mapTilAktivitetDTO(eksisterende, true)

            val exception = assertThrows<ResponseStatusException> {
                controller.oppdaterAktivitet(dto)
            }
            assertThat(exception.statusCode.value()).isEqualTo(400)
            verify(aktivitetDAO, never()).oppdaterAktivitet(any())
        }

        @Test
        fun `oppdater EGEN som ekstern - avtalt - avvises`() {
            setupEksternBrukerContextForOppdater()
            val eksisterende = AktivitetDataTestBuilder.nyEgenaktivitet().toBuilder()
                .id(58L).versjon(2L).avtalt(true).status(AktivitetStatus.PLANLAGT).historiskDato(null).build()
            `when`(aktivitetDAO.hentAktivitet(eksisterende.id)).thenReturn(eksisterende)

            val dto = AktivitetDTOMapper.mapTilAktivitetDTO(eksisterende, true)

            val exception = assertThrows<ResponseStatusException> {
                controller.oppdaterAktivitet(dto)
            }
            assertThat(exception.statusCode.value()).isEqualTo(400)
            verify(aktivitetDAO, never()).oppdaterAktivitet(any())
        }
    }
}
