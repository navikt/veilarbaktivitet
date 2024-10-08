package no.nav.veilarbaktivitet.oppfolging.periode

import no.nav.common.json.JsonUtils
import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions
import no.nav.veilarbaktivitet.person.Innsender
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
        assertThat(output.oppfolgingsperiodeId).isEqualTo(id)
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
        val mockBruker = navMockService.createBruker(BrukerOptions.happyBruker().toBuilder().underOppfolging(false).build())
        val startOppfolgiong = SisteOppfolgingsperiodeV1.builder()
            .uuid(mockBruker.getOppfolgingsperiodeId())
            .aktorId(mockBruker.aktorId.get())
            .startDato(ZonedDateTime.now().minusHours(1).truncatedTo(ChronoUnit.MILLIS))
            .build()
        val sendResult = producer.send(
            oppfolgingSistePeriodeTopic,
            mockBruker.aktorId.get(),
            JsonUtils.toJson(startOppfolgiong)
        )[1, TimeUnit.SECONDS]
        kafkaTestService.assertErKonsumertNavCommon(oppfolgingSistePeriodeTopic, sendResult.recordMetadata.offset())
        val oppfolgingsperiode = sistePeriodeDAO.hentSisteOppfolgingsPeriode(mockBruker.aktorId).orElseThrow()
        assertThat(oppfolgingsperiode.oppfolgingsperiodeId).isEqualTo(mockBruker.getOppfolgingsperiodeId())
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
        kafkaTestService.assertErKonsumertNavCommon(oppfolgingSistePeriodeTopic, avsluttetSendResult.recordMetadata.offset())
        val oppfolgingsperiodeAvsluttet = sistePeriodeDAO.hentSisteOppfolgingsPeriode(mockBruker.aktorId).orElseThrow()
        assertThat(oppfolgingsperiodeAvsluttet.oppfolgingsperiodeId)
            .isEqualTo(mockBruker.getOppfolgingsperiodeId())
        assertThat(oppfolgingsperiodeAvsluttet.aktorid).isEqualTo(mockBruker.aktorId.get())
        assertThat(oppfolgingsperiodeAvsluttet.startTid).isEqualTo(avsluttetOppfolgingsperide.getStartDato())
        assertThat(oppfolgingsperiodeAvsluttet.sluttTid).isEqualTo(avsluttetOppfolgingsperide.getSluttDato())
        // Sjekk at ny DAO også gir samme svar
        val oppfolg = oppfolgingsperiodeDAO.getByAktorId(mockBruker.aktorId).first()
        assertThat(oppfolg.oppfolgingsperiodeId).isEqualTo(mockBruker.getOppfolgingsperiodeId())
        assertThat(oppfolg.aktorid).isEqualTo(mockBruker.aktorId.get())
        assertThat(oppfolg.startTid).isEqualTo(avsluttetOppfolgingsperide.getStartDato())
        assertThat(oppfolg.sluttTid).isEqualTo(avsluttetOppfolgingsperide.getSluttDato())
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun skal_sette_aktiviteteter_til_hitorisk_naar_oppfolging_avsluttes() {
        val brukerUteAvOppfolging = navMockService.createHappyBruker()
        val aktivitet = AktivitetDTOMapper.mapTilAktivitetDTO(AktivitetDataTestBuilder.nyEgenaktivitet(), false)
        val skalBliHistorisk = aktivitetTestService.opprettAktivitet(brukerUteAvOppfolging, aktivitet)
        val oppfolgingsperiodeSkalAvsluttes = brukerUteAvOppfolging.getOppfolgingsperiodeId()
        navMockService.newOppfolingsperiode(brukerUteAvOppfolging)
        val skalIkkeBliHistorisk = aktivitetTestService.opprettAktivitet(brukerUteAvOppfolging, aktivitet)

        // Avslutt oppfølging
        val avsluttOppfolging = SisteOppfolgingsperiodeV1.builder()
            .uuid(oppfolgingsperiodeSkalAvsluttes)
            .aktorId(brukerUteAvOppfolging.aktorId.get())
            .startDato(ZonedDateTime.now().minusHours(2).truncatedTo(ChronoUnit.MILLIS))
            .sluttDato(ZonedDateTime.now().minusHours(1).truncatedTo(ChronoUnit.MILLIS))
            .build()
        val sendResult = producer.send(oppfolgingSistePeriodeTopic, JsonUtils.toJson(avsluttOppfolging)).get()
        kafkaTestService.assertErKonsumertNavCommon(oppfolgingSistePeriodeTopic, sendResult.recordMetadata.offset())
        // Send 2 ganger for å teste idempotens
        val sendResult2 = producer.send(oppfolgingSistePeriodeTopic, JsonUtils.toJson(avsluttOppfolging)).get()
        kafkaTestService.assertErKonsumertNavCommon(oppfolgingSistePeriodeTopic, sendResult2.recordMetadata.offset())

        val aktiviteter = aktivitetTestService.hentAktiviteterForFnr(brukerUteAvOppfolging).aktiviteter
        val skalVaereHistoriskVersioner = aktivitetTestService.hentVersjoner(skalBliHistorisk.id, brukerUteAvOppfolging, brukerUteAvOppfolging)

        // aktivitet som skal være historisk
        val skalVaereHistorisk = aktiviteter.first { a: AktivitetDTO -> a.id == skalBliHistorisk.id }
        AktivitetAssertUtils.assertOppdatertAktivitet(skalBliHistorisk.setHistorisk(true), skalVaereHistorisk)
        assertEquals(AktivitetTransaksjonsType.BLE_HISTORISK, skalVaereHistorisk.transaksjonsType)
        assertThat(skalVaereHistorisk.endretAvType).isEqualTo(Innsender.SYSTEM.name)
        assertThat(skalVaereHistoriskVersioner).hasSize(2)
        assertThat(skalVaereHistoriskVersioner.last().endretDato).isNotEqualTo(skalVaereHistoriskVersioner.first().endretDato)

        // aktiviteter som ikke skal være historisk
        val skalIkkeVaereHistorisk = aktiviteter.first { a: AktivitetDTO -> a.id == skalIkkeBliHistorisk.id }
        assertEquals(skalIkkeBliHistorisk, skalIkkeVaereHistorisk)
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
