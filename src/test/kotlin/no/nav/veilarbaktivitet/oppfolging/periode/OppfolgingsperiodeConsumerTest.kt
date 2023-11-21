package no.nav.veilarbaktivitet.oppfolging.periode

import no.nav.common.json.JsonUtils
import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService
import no.nav.veilarbaktivitet.person.Person
import no.nav.veilarbaktivitet.testutils.AktivitetAssertUtils
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
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
    fun `Skal oppdatere til på oppfølgingsperiode ved upsert på eksisterende åpen periode`() {
        val aktorId = Person.aktorId("12121231313")
        val start = ZonedDateTime.now().truncatedTo(ChronoUnit.MILLIS)
        val slutt = ZonedDateTime.now().plusDays(2).truncatedTo(ChronoUnit.MILLIS)
        val id = UUID.randomUUID()
        val oppfolging = Oppfolgingsperiode(aktorId.get(), id, start, null)
        val nyOppfolging = Oppfolgingsperiode(aktorId.get(), id, start, slutt)
        oppfolgingsperiodeDAO.upsertOppfolgingsperide(oppfolging)
        val output = oppfolgingsperiodeDAO.getByAktorId(aktorId).first()
        assertThat(output.oppfolgingsperiode).isEqualTo(id)
        assertThat(output.aktorid).isEqualTo(aktorId.get())
        assertThat(output.startTid).isEqualTo(start)
        assertThat(output.sluttTid).isNull()
        oppfolgingsperiodeDAO.upsertOppfolgingsperide(nyOppfolging)
        assertThat(oppfolgingsperiodeDAO.getByAktorId(aktorId).first().sluttTid).isEqualTo(slutt)
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
        val sendResult = producer.send(
            oppfolgingSistePeriodeTopic,
            mockBruker.aktorId.get(),
            JsonUtils.toJson(startOppfolgiong)
        )[1, TimeUnit.SECONDS]
        kafkaTestService.assertErKonsumert(oppfolgingSistePeriodeTopic, sendResult.recordMetadata.offset())
        val oppfolgingsperiode = sistePeriodeDAO.hentSisteOppfolgingsPeriode(mockBruker.aktorId).orElseThrow()
        assertThat(oppfolgingsperiode.oppfolgingsperiode).isEqualTo(mockBruker.getOppfolgingsperiode())
        assertThat(oppfolgingsperiode.aktorid).isEqualTo(mockBruker.aktorId.get())
        assertThat(oppfolgingsperiode.startTid).isEqualTo(startOppfolgiong.getStartDato())
        assertThat(oppfolgingsperiode.sluttTid).isNull()
        val avsluttetOppfolgingsperide =
            startOppfolgiong.withSluttDato(ZonedDateTime.now().truncatedTo(ChronoUnit.MILLIS))
        val avsluttetSendResult = producer.send(
            oppfolgingSistePeriodeTopic,
            mockBruker.aktorId.get(),
            JsonUtils.toJson(avsluttetOppfolgingsperide)
        )[1, TimeUnit.SECONDS]
        kafkaTestService.assertErKonsumert(oppfolgingSistePeriodeTopic, avsluttetSendResult.recordMetadata.offset())
        val oppfolgingsperiodeAvsluttet = sistePeriodeDAO.hentSisteOppfolgingsPeriode(mockBruker.aktorId).orElseThrow()
        assertThat(oppfolgingsperiodeAvsluttet.oppfolgingsperiode)
            .isEqualTo(mockBruker.getOppfolgingsperiode())
        assertThat(oppfolgingsperiodeAvsluttet.aktorid).isEqualTo(mockBruker.aktorId.get())
        assertThat(oppfolgingsperiodeAvsluttet.startTid).isEqualTo(avsluttetOppfolgingsperide.getStartDato())
        assertThat(oppfolgingsperiodeAvsluttet.sluttTid).isEqualTo(avsluttetOppfolgingsperide.getSluttDato())
        // Sjekk at ny DAO også gir samme svar
        val oppfolg = oppfolgingsperiodeDAO.getByAktorId(mockBruker.aktorId).first()
        assertThat(oppfolg.oppfolgingsperiode).isEqualTo(mockBruker.getOppfolgingsperiode())
        assertThat(oppfolg.aktorid).isEqualTo(mockBruker.aktorId.get())
        assertThat(oppfolg.startTid).isEqualTo(avsluttetOppfolgingsperide.getStartDato())
        assertThat(oppfolg.sluttTid).isEqualTo(avsluttetOppfolgingsperide.getSluttDato())
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
        val sendResult = producer.send(oppfolgingSistePeriodeTopic, JsonUtils.toJson(avsluttOppfolging)).get()
        kafkaTestService.assertErKonsumert(oppfolgingSistePeriodeTopic, sendResult.recordMetadata.offset())
        val aktiviteter = aktivitetTestService.hentAktiviteterForFnr(mockBruker).aktiviteter
        val skalVaereHistorisk = aktiviteter.stream().filter { a: AktivitetDTO -> a.id == skalBliHistorisk.id }
            .findAny().get()
        AktivitetAssertUtils.assertOppdatertAktivitet(skalBliHistorisk.setHistorisk(true), skalVaereHistorisk)
        assertEquals(AktivitetTransaksjonsType.BLE_HISTORISK, skalVaereHistorisk.transaksjonsType)
        val skalIkkeVaereHistorisk = aktiviteter.stream().filter { a: AktivitetDTO -> a.id == skalIkkeBliHistorisk.id }
            .findAny().get()
        assertEquals(skalIkkeBliHistorisk, skalIkkeVaereHistorisk)
        val skalIkkeVaereHistoriskMockBruker2 = aktivitetTestService.hentAktiviteterForFnr(mockBruker2).aktiviteter[0]
        assertEquals(skalIkkeBliHistoriskMockBruker2, skalIkkeVaereHistoriskMockBruker2)
    }

    @Test
    fun skal_gi_ut_oppfolgingsperiode_i_riktig_rekkefolge() {
        val aktorId = Person.aktorId("12345678901")
        val start = ZonedDateTime.now().minusMonths(12).truncatedTo(ChronoUnit.MILLIS)
        val slutt = ZonedDateTime.now().minusMonths(11).truncatedTo(ChronoUnit.MILLIS)
        val oppfolging1 = Oppfolgingsperiode(aktorId.get(), UUID.randomUUID(), start, slutt)
        val oppfolging2 = Oppfolgingsperiode(aktorId.get(), UUID.randomUUID(), slutt.plusMonths(1), slutt.plusMonths(5))
        val oppfolging3 = Oppfolgingsperiode(aktorId.get(), UUID.randomUUID(), slutt.plusMonths(10), null)
        oppfolgingsperiodeDAO.upsertOppfolgingsperide(oppfolging1)
        oppfolgingsperiodeDAO.upsertOppfolgingsperide(oppfolging2)
        oppfolgingsperiodeDAO.upsertOppfolgingsperide(oppfolging3)
        val output = oppfolgingsperiodeDAO.getByAktorId(aktorId)
        assertThat(output).containsExactly(oppfolging3, oppfolging2, oppfolging1)
    }
}
