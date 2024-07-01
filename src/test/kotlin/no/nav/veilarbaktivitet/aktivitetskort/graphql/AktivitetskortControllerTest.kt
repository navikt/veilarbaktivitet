package no.nav.veilarbaktivitet.aktivitetskort.graphql

import no.nav.common.json.JsonMapper
import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder
import no.nav.veilarbaktivitet.util.DateUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
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
        assertThat(result.body.asString()).isEqualTo(
            """
            {"message":"Kan ikke endre aktivitet i en ferdig status","statusCode":400}
        """.trimIndent()
        )
    }

    @Test
    fun `skal kunne hente historikk`() {
        val nyPeriode = UUID.randomUUID()
        val jobbAktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.IJOBB)
            .toBuilder().oppfolgingsperiodeId(nyPeriode).build()
        val aktivitet = aktivitetTestService.opprettAktivitet(mockBruker, mockBruker, jobbAktivitet)
        aktivitetTestService.oppdaterAktivitetStatus(mockBruker, mockVeileder, aktivitet, AktivitetStatus.GJENNOMFORES)
        val aktivitetIdParam = "\$aktivitetId"
        val query = """
            query($aktivitetIdParam: String) {
                aktivitet(aktivitetId: $aktivitetIdParam) {
                    historikk {
                        endringer {
                            endretAvType,
                            endretAv,
                            tidspunkt,
                            beskrivelseForVeileder,
                            beskrivelseForBruker,
                            beskrivelseForArkiv,
                        }
                    }
                }
            }
        """.trimIndent().replace("\n", "")
        val result = aktivitetTestService.queryHistorikk(mockBruker, mockBruker, query, aktivitet.id.toLong())
        assertThat(result.errors).isNull()
        assertThat(result.data?.aktivitet?.historikk).isNotNull()
        assertThat(result.data?.aktivitet?.historikk?.endringer).hasSize(2)
    }

    @Test
    fun `skal serilisere datoer riktig (aktivitet)`() {
        val nyPeriode = UUID.randomUUID()
        // riktig:  "2024-04-30T22:00:00.000+00:00"
        // feil:    "2024-05-01T00:00+02:00"
        val fraDatoIso = "2024-04-30T22:00:00.000+00:00"
        val jobbAktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.IJOBB)
            .toBuilder().oppfolgingsperiodeId(nyPeriode).fraDato(DateUtils.dateFromISO8601(fraDatoIso)).build()
        val aktivitet = aktivitetTestService.opprettAktivitet(mockBruker, mockBruker, jobbAktivitet)
        aktivitetTestService.oppdaterAktivitetStatus(mockBruker, mockVeileder, aktivitet, AktivitetStatus.GJENNOMFORES)
        val aktivitetIdParam = "\$aktivitetId"
        val query = """
            query($aktivitetIdParam: String) {
                aktivitet(aktivitetId: $aktivitetIdParam) {
                    fraDato
                }
            }
        """.trimIndent().replace("\n", "")
        val result = aktivitetTestService.queryHistorikkRaw(mockBruker, mockBruker, query, aktivitet.id.toLong())
        val fraDatoString = JsonMapper.defaultObjectMapper().readTree(result)["data"]["aktivitet"]["fraDato"]
        assertThat(fraDatoString).isEqualTo(fraDatoIso)
    }

    @Test
    fun `skal serilisere datoer riktig (perioder)`() {
        val nyPeriode = UUID.randomUUID()
        // riktig:  "2024-04-30T22:00:00.000+00:00"
        // feil:    "2024-05-01T00:00+02:00"
        val fraDatoIso = "2024-04-30T22:00:00.000+00:00"
        val jobbAktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.IJOBB)
            .toBuilder().oppfolgingsperiodeId(nyPeriode).fraDato(DateUtils.dateFromISO8601(fraDatoIso)).build()
        val aktivitet = aktivitetTestService.opprettAktivitet(mockBruker, mockBruker, jobbAktivitet)
        aktivitetTestService.oppdaterAktivitetStatus(mockBruker, mockVeileder, aktivitet, AktivitetStatus.GJENNOMFORES)
        val fnrParam = "\$fnr"
        val aktivitetIdParam = "\$aktivitetId"
        val query = """
            query($fnrParam: String!, $aktivitetIdParam: String!) {
                perioder(fnr: $fnrParam) { 
                    id,
                    aktiviteter { fraDato }
                }
                aktivitet(aktivitetId: $aktivitetIdParam) {
                    fraDato
                }
            }
        """.trimIndent().replace("\n", "")
        val result = aktivitetTestService.queryAllRaw(mockBruker, mockBruker, query, aktivitet.id.toLong())
        val jsonTree = JsonMapper.defaultObjectMapper().readTree(result)
        val fraDatoStringAktivitet = jsonTree["data"]["aktivitet"]["fraDato"]
        val fraDatoStringPerider = jsonTree["data"]["perioder"][0]["aktiviteter"][0]["fraDato"]
        assertThat(fraDatoStringAktivitet).isEqualTo(fraDatoStringPerider)
        assertThat(fraDatoStringAktivitet).isEqualTo(fraDatoIso)
        assertThat(fraDatoStringPerider).isEqualTo(fraDatoIso)
    }

}
