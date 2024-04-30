package no.nav.veilarbaktivitet.aktivitetskort.graphql

import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.AktivitetsplanController
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.arkivering.ArkiveringsController
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.*

class AktivitetskortControllerTest: SpringBootTestBase() {

    private val mockBruker by lazy { navMockService.createHappyBruker() }
    private val mockVeileder: MockVeileder by lazy { MockNavService.createVeileder(mockBruker) }

    @Test
    fun `skal gruppere pa oppfolgingsperiode (bruker)`() {
        // Escaping $ does not work in multiline strings so use variable instead
        val fnrParam = "\$fnr"
        val query = """
            query($fnrParam: String!) {
                perioder(fnr: $fnrParam) { 
                    id,
                    aktiviteter {
                        id,
                        opprettetDato
                    }
                }
            }
        """.trimIndent().replace("\n", "")

        val gammelPeriode = UUID.randomUUID()
        val nyPeriode = UUID.randomUUID()
        val jobbAktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.IJOBB)
            .toBuilder().oppfolgingsperiodeId(gammelPeriode).build()
        aktivitetTestService.opprettAktivitet(mockBruker, mockBruker, jobbAktivitet)
        val aktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN)
            .toBuilder().oppfolgingsperiodeId(nyPeriode).build()
        aktivitetTestService.opprettAktivitet(mockBruker, mockBruker, aktivitet)
        val result = aktivitetTestService.queryAktivitetskort(mockBruker, mockBruker, query)
        assertThat(result.errors).isNull()
        assertThat(result.data?.perioder).hasSize(2)
    }

    @Test
    fun `skal funke for veileder`() {
        // Escaping $ does not work in multiline strings so use variable instead
        val fnrParam = "\$fnr"
        val query = """
            query($fnrParam: String!) {
                perioder(fnr: $fnrParam) { 
                    id,
                    aktiviteter {
                        id
                    }
                }
            }
        """.trimIndent().replace("\n", "")
        val jobbAktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.IJOBB)
        aktivitetTestService.opprettAktivitet(mockBruker, mockVeileder, jobbAktivitet)
        val result = aktivitetTestService.queryAktivitetskort(mockBruker, mockVeileder, query)
        assertThat(result.errors).isNull()
        assertThat(result.data?.perioder).hasSize(1)
    }

    @Test
    fun `veileder skal ikke ha tilgang til aktiviteter hvis ikke tilgang på bruker`() {
        val bruker = MockNavService.createHappyBruker()
        // Escaping $ does not work in multiline strings so use variable instead
        val fnrParam = "\$fnr"
        val query = """
            query($fnrParam: String!) {
                perioder(fnr: $fnrParam) { 
                    id,
                    aktiviteter { id }
                }
            }
        """.trimIndent().replace("\n", "")
        val jobbAktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.IJOBB)
        aktivitetTestService.opprettAktivitet(bruker, bruker, jobbAktivitet)
        val result = aktivitetTestService.queryAktivitetskort(bruker, mockVeileder, query)
        assertThat(result.data?.perioder).isNull()
        assertThat(result.errors!!.first().message).isEqualTo("Ikke tilgang")
    }

    @Test
    fun `veileder skal ikke ha tilgang på kvp aktiviteter hvis ikke tilgang på enhet`() {
        val kvpBruker = MockNavService.createBruker(
            BrukerOptions.happyBruker().toBuilder()
                .erUnderKvp(true)
                .build()
        )
        val veileder = MockNavService.createVeilederMedNasjonalTilgang()
        // Escaping $ does not work in multiline strings so use variable instead
        val fnrParam = "\$fnr"
        val query = """
            query($fnrParam: String!) {
                perioder(fnr: $fnrParam) { 
                    id,
                    aktiviteter { id }
                }
            }
        """.trimIndent().replace("\n", "")
        val jobbAktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.IJOBB)
        aktivitetTestService.opprettAktivitet(kvpBruker, kvpBruker, jobbAktivitet)
        val result = aktivitetTestService.queryAktivitetskort(kvpBruker, veileder, query)
        assertThat(result.data?.perioder).isEmpty()
        assertThat(result.errors).isNull()
    }

    @Test
    fun `skal serialisere feil`() {
        val fnrParam = "\$fnr"
        val query = """
            query($fnrParam: String!) {
                lol
            }
        """.trimIndent().replace("\n", "")
        val result = aktivitetTestService.queryAktivitetskort(mockBruker, mockBruker, query)
        assertThat(result.errors).hasSize(2)
        assertThat(result.errors?.get(0)?.message?.contains("Field 'lol' in type 'Query' is undefined")).isTrue()
        assertThat(result.errors?.get(1)?.message?.contains("Validation error (UnusedVariable) : Unused variable 'fnr'")).isTrue()
        assertThat(result.data).isNull()
    }

    @Test
    fun `Skal gi bad request når man endrer status på en fullført aktivitet`() {
        val aktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE).setStatus(AktivitetStatus.FULLFORT)
        val opprettetAktivitet = aktivitetTestService.opprettAktivitet(mockBruker, mockVeileder, aktivitet)

        val result = mockVeileder
            .createRequest(mockBruker)
            .body(opprettetAktivitet)
            .put("http://localhost:$port/veilarbaktivitet/api/aktivitet/${opprettetAktivitet.id}/status")

        assertThat(result.statusCode).isEqualTo(400)
        assertThat(result.body.asString()).isEqualTo("""
            {"message":"Kan ikke endre aktivitet i en ferdig status","statusCode":400}
        """.trimIndent())
    }

}
