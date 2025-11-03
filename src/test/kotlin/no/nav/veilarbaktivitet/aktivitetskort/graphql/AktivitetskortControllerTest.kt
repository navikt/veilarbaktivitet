package no.nav.veilarbaktivitet.aktivitetskort.graphql

import no.nav.common.json.JsonMapper
import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder
import no.nav.veilarbaktivitet.oppfolging.periode.SisteOppfolgingsperiodeV1
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder
import no.nav.veilarbaktivitet.util.DateUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class AktivitetskortControllerTest: SpringBootTestBase() {

    private val mockBruker by lazy { navMockService.createBruker() }
    private val mockVeileder: MockVeileder by lazy { navMockService.createVeileder(mockBruker) }

    @Test
    fun `skal gruppere pa oppfolgingsperiode (bruker)`() {
        // Escaping $ does not work in multiline strings so use variable instead
        val fnrParam = "\$fnr"
        val query = """
            query($fnrParam: String!) {
                perioder(fnr: $fnrParam) { 
                    id,
                    start,
                    slutt,
                    aktiviteter {
                        id,
                        opprettetDato
                    }
                }
            }
        """.trimIndent().replace("\n", "")

        val gammelPeriodeId = UUID.randomUUID()
        val gammelperiodeStart = ZonedDateTime.now().minusYears(5)
        val gammelperiodeSlutt = gammelperiodeStart
        val nyPeriode = mockBruker.oppfolgingsperiodeId
        val jobbAktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.IJOBB)
            .toBuilder().oppfolgingsperiodeId(gammelPeriodeId).build()
        aktivitetTestService.opprettAktivitet(mockBruker, mockBruker, jobbAktivitet)
        val aktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN)
            .toBuilder().oppfolgingsperiodeId(nyPeriode).build()
        aktivitetTestService.upsertOppfolgingsperiode(SisteOppfolgingsperiodeV1.builder()
            .uuid(gammelPeriodeId)
            .aktorId(mockBruker.aktorId.get())
            .startDato(gammelperiodeStart)
            .sluttDato(gammelperiodeSlutt)
            .build()
        )
        aktivitetTestService.opprettAktivitet(mockBruker, mockBruker, aktivitet)
        val result = aktivitetTestService.queryAktivitetskort(mockBruker, mockBruker, query)
        val nyestePeriode = result.data?.perioder?.first()
        val gammelPeriode = result.data?.perioder?.last()
        assertThat(result.errors).isNull()
        assertThat(result.data?.perioder).hasSize(2)
        assertThat(nyestePeriode?.start).isCloseTo(mockBruker.oppfolgingsperioder.first().startTid, within(1, ChronoUnit.MILLIS))
        assertThat(nyestePeriode?.slutt).isNull()
        assertThat(gammelPeriode?.start).isCloseTo(gammelperiodeStart, within(1, ChronoUnit.MILLIS))
        assertThat(gammelPeriode?.start).isCloseTo(gammelperiodeSlutt, within(1, ChronoUnit.MILLIS))
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
        val bruker = navMockService.createHappyBruker()
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
        val kvpBruker = navMockService.createBruker(
            BrukerOptions.happyBruker().toBuilder()
                .erUnderKvp(true)
                .build()
        )
        val veileder = navMockService.createVeilederMedNasjonalTilgang()
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
        assertThat(result.data?.perioder?.firstOrNull()?.aktiviteter).isEmpty()
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
    fun `skal kunne hente eier av aktivitetskort`() {
        val jobbAktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.IJOBB)
            .toBuilder().oppfolgingsperiodeId(mockBruker.oppfolgingsperiodeId).build()
        val aktivitet = aktivitetTestService.opprettAktivitet(mockBruker, mockBruker, jobbAktivitet)
        val aktivitetIdParam = "\$aktivitetId"
        val query = """
            query($aktivitetIdParam: String!) {
                aktivitet(aktivitetId: $aktivitetIdParam) {
                   tittel                   
                },
                eier(aktivitetId: $aktivitetIdParam) {
                    fnr
                }
            }
        """.trimIndent().replace("\n", "")
        val result = aktivitetTestService.queryHentEier(mockVeileder , query, aktivitet.id)
        assertThat(result.errors).isNull()
        assertThat(result.data?.eier).isNotNull()
        assertThat(result.data?.eier?.fnr).isEqualTo(mockBruker.fnr)
    }

    @Test
    fun `skal kunne hente historikk`() {
        val jobbAktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.IJOBB)
            .toBuilder().oppfolgingsperiodeId(mockBruker.oppfolgingsperiodeId).build()
        val aktivitet = aktivitetTestService.opprettAktivitet(mockBruker, mockBruker, jobbAktivitet)
        aktivitetTestService.oppdaterAktivitetStatus(mockBruker, mockVeileder, aktivitet, AktivitetStatus.GJENNOMFORES)
        val aktivitetIdParam = "\$aktivitetId"
        val query = """
            query($aktivitetIdParam: String!) {
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
        val result = aktivitetTestService.queryHistorikk(mockBruker, mockBruker, query, aktivitet.id)
        assertThat(result.errors).isNull()
        assertThat(result.data?.aktivitet?.historikk).isNotNull()
        assertThat(result.data?.aktivitet?.historikk?.endringer).hasSize(2)
    }

    @Test
    fun `skal serialisere datoer riktig (aktivitet)`() {
        // riktig:  "2024-04-30T22:00:00.000+00:00"
        // feil:    "2024-05-01T00:00+02:00"
        val fraDatoIso = "2024-04-30T22:00:00.000+00:00"
        val jobbAktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.IJOBB)
            .toBuilder().oppfolgingsperiodeId(mockBruker.oppfolgingsperiodeId).fraDato(DateUtils.dateFromISO8601(fraDatoIso)).build()
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
        val result = aktivitetTestService.queryHistorikkRaw(mockBruker, mockBruker, query, aktivitet.id)
        val fraDatoString = JsonMapper.defaultObjectMapper().readTree(result)["data"]["aktivitet"]["fraDato"].asText()
        assertThat(fraDatoString).isEqualTo(fraDatoIso)
    }

    @Test
    fun `skal serialisere datoer riktig (perioder)`() {
        // riktig:  "2024-04-30T22:00:00.000+00:00"
        // feil:    "2024-05-01T00:00+02:00"
        val fraDatoIso = "2024-04-30T22:00:00.000+00:00"
        val jobbAktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.IJOBB)
            .toBuilder().oppfolgingsperiodeId(mockBruker.oppfolgingsperiodeId).fraDato(DateUtils.dateFromISO8601(fraDatoIso)).build()
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
        val fraDatoStringAktivitet = jsonTree["data"]["aktivitet"]["fraDato"].asText()
        val fraDatoStringPerioder = jsonTree["data"]["perioder"][0]["aktiviteter"][0]["fraDato"].asText()
        assertThat(fraDatoStringAktivitet).isEqualTo(fraDatoStringPerioder)
        assertThat(fraDatoStringAktivitet).isEqualTo(fraDatoIso)
        assertThat(fraDatoStringPerioder).isEqualTo(fraDatoIso)
    }

    @Test
    fun `skal serialisere som zonedatetime uten EuropeOslo som suffix (historikk)`() {
        // riktig: 2024-04-30T23:00:00.000+02:00
        // feil: 2024-04-30T23:00:00.000+02:00[Europe/Oslo]
        val nyPeriode = UUID.randomUUID()
        val fraDatoIso = "2024-04-30T21:00:00.000+00:00"
        val jobbAktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.IJOBB)
            .toBuilder().oppfolgingsperiodeId(nyPeriode).fraDato(DateUtils.dateFromISO8601(fraDatoIso)).build()
        val aktivitet = aktivitetTestService.opprettAktivitet(mockBruker, mockBruker, jobbAktivitet)
        aktivitetTestService.oppdaterAktivitetStatus(mockBruker, mockVeileder, aktivitet, AktivitetStatus.GJENNOMFORES)
        val aktivitetIdParam = "\$aktivitetId"
        val query = """
            query($aktivitetIdParam: String!) {
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
        val result = aktivitetTestService.queryHistorikkRaw(mockBruker, mockBruker, query, aktivitet.id)
        val fraDatoString = JsonMapper.defaultObjectMapper().readTree(result)["data"]["aktivitet"]["historikk"]["endringer"][0]["tidspunkt"].asText()

        val matchMedTidssone = fraDatoString.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}T([0-9]{2}:){2}[0-9]{2}\\.[0-9]{3}[+|-][0-9]{2}:[0-9]{2}$"))
        val matchZulu = fraDatoString.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}T([0-9]{2}:){2}[0-9]{2}\\.[0-9]{3}[Z]$"))
        assertThat(matchMedTidssone || matchZulu).isTrue()
    }

}
