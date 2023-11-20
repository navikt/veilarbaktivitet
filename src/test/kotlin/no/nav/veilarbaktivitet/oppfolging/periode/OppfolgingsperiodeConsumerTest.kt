package no.nav.veilarbaktivitet.oppfolging.periode

import no.nav.common.json.JsonUtils
import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService
import no.nav.veilarbaktivitet.testutils.AktivitetAssertUtils
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

internal class OppfolgingsperiodeConsumerTest : SpringBootTestBase() {
    @Autowired
    lateinit var producer: KafkaTemplate<String, String>

    @Value("\${topic.inn.oppfolgingsperiode}")
    lateinit var oppfolgingSistePeriodeTopic: String

    @Autowired
    lateinit var sistePeriodeDAO: SistePeriodeDAO

    @Autowired
    lateinit var oppfolgingsperiodeDAO: OppfolgingsperiodeDAO

    @Test
    @Throws()
    fun `test merge`() {

        val oppfolging = Oppfolgingsperiode("1", UUID.randomUUID(), ZonedDateTime.now(), null )
        val nyOppfolging = Oppfolgingsperiode("1", UUID.randomUUID(), ZonedDateTime.now(), ZonedDateTime.now().plusDays(2) )
        oppfolgingsperiodeDAO.upsertOppfolgingsperide(oppfolging)
        oppfolgingsperiodeDAO.upsertOppfolgingsperide(nyOppfolging)

    }
    @Test
    @Throws(
        InterruptedException::class,
        ExecutionException::class,
        TimeoutException::class
    )
    fun skal_opprette_siste_oppfolgingsperiode() {
        val mockBruker = MockNavService.createHappyBruker()
        val startOppfolgiong = SisteOppfolgingsperiodeV1.builder()
            .uuid(mockBruker.getOppfolgingsperiode())
            .aktorId(mockBruker.aktorId.get())
            .startDato(ZonedDateTime.now().minusHours(1).truncatedTo(ChronoUnit.MILLIS))
            .build()
        val sendResult = producer!!.send(
            oppfolgingSistePeriodeTopic,
            mockBruker.aktorId.get(),
            JsonUtils.toJson(startOppfolgiong)
        )[1, TimeUnit.SECONDS]
        kafkaTestService.assertErKonsumert(oppfolgingSistePeriodeTopic, sendResult.recordMetadata.offset())
        val oppfolgingsperiode = sistePeriodeDAO!!.hentSisteOppfolgingsPeriode(mockBruker.aktorId).orElseThrow()
        Assertions.assertThat(oppfolgingsperiode.oppfolgingsperiode).isEqualTo(mockBruker.getOppfolgingsperiode())
        Assertions.assertThat(oppfolgingsperiode.aktorid).isEqualTo(mockBruker.aktorId.get())
        Assertions.assertThat(oppfolgingsperiode.startTid).isEqualTo(startOppfolgiong.getStartDato())
        Assertions.assertThat(oppfolgingsperiode.sluttTid).isNull()
        val avsluttetOppfolgingsperide =
            startOppfolgiong.withSluttDato(ZonedDateTime.now().truncatedTo(ChronoUnit.MILLIS))
        val avsluttetSendResult = producer!!.send(
            oppfolgingSistePeriodeTopic,
            mockBruker.aktorId.get(),
            JsonUtils.toJson(avsluttetOppfolgingsperide)
        )[1, TimeUnit.SECONDS]
        kafkaTestService.assertErKonsumert(oppfolgingSistePeriodeTopic, avsluttetSendResult.recordMetadata.offset())
        val oppfolgingsperiodeAvsluttet = sistePeriodeDAO.hentSisteOppfolgingsPeriode(mockBruker.aktorId).orElseThrow()
        Assertions.assertThat(oppfolgingsperiodeAvsluttet.oppfolgingsperiode)
            .isEqualTo(mockBruker.getOppfolgingsperiode())
        Assertions.assertThat(oppfolgingsperiodeAvsluttet.aktorid).isEqualTo(mockBruker.aktorId.get())
        Assertions.assertThat(oppfolgingsperiodeAvsluttet.startTid).isEqualTo(avsluttetOppfolgingsperide.getStartDato())
        Assertions.assertThat(oppfolgingsperiodeAvsluttet.sluttTid).isEqualTo(avsluttetOppfolgingsperide.getSluttDato())
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun skal_avslutte_aktiviteter_for() {
        val mockBruker = MockNavService.createHappyBruker()
        val mockBruker2 = MockNavService.createHappyBruker()
        val aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet()
        val aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false)
        val skalIkkeBliHistoriskMockBruker2 = aktivitetTestService.opprettAktivitet(mockBruker2, aktivitetDTO)
        val skalBliHistorisk = aktivitetTestService.opprettAktivitet(mockBruker, aktivitetDTO)
        val oppfolgingsperiodeSkalAvsluttes = mockBruker.getOppfolgingsperiode()
        MockNavService.newOppfolingsperiode(mockBruker)
        val skalIkkeBliHistorisk = aktivitetTestService.opprettAktivitet(mockBruker, aktivitetDTO)
        val avsluttOppfolging = SisteOppfolgingsperiodeV1.builder()
            .uuid(oppfolgingsperiodeSkalAvsluttes)
            .aktorId(mockBruker.aktorId.get())
            .startDato(ZonedDateTime.now().minusHours(1).truncatedTo(ChronoUnit.MILLIS))
            .sluttDato(ZonedDateTime.now().minusHours(1).truncatedTo(ChronoUnit.MILLIS))
            .build()
        val sendResult = producer!!.send(oppfolgingSistePeriodeTopic, JsonUtils.toJson(avsluttOppfolging)).get()
        kafkaTestService.assertErKonsumert(oppfolgingSistePeriodeTopic, sendResult.recordMetadata.offset())
        val aktiviteter = aktivitetTestService.hentAktiviteterForFnr(mockBruker).aktiviteter
        val skalVaereHistorisk = aktiviteter.stream().filter { a: AktivitetDTO -> a.id == skalBliHistorisk.id }
            .findAny().get()
        AktivitetAssertUtils.assertOppdatertAktivitet(skalBliHistorisk.setHistorisk(true), skalVaereHistorisk)
        org.junit.jupiter.api.Assertions.assertEquals(
            AktivitetTransaksjonsType.BLE_HISTORISK,
            skalVaereHistorisk.transaksjonsType
        )
        val skalIkkeVaereHistorisk = aktiviteter.stream().filter { a: AktivitetDTO -> a.id == skalIkkeBliHistorisk.id }
            .findAny().get()
        org.junit.jupiter.api.Assertions.assertEquals(skalIkkeBliHistorisk, skalIkkeVaereHistorisk)
        val skalIkkeVaereHistoriskMockBruker2 = aktivitetTestService.hentAktiviteterForFnr(mockBruker2).aktiviteter[0]
        org.junit.jupiter.api.Assertions.assertEquals(
            skalIkkeBliHistoriskMockBruker2,
            skalIkkeVaereHistoriskMockBruker2
        )
    }
}
