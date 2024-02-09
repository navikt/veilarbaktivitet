package no.nav.veilarbaktivitet.aktivitetskort.util

import lombok.SneakyThrows
import no.nav.veilarbaktivitet.aktivitetskort.ActionType
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetsbestillingCreator
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortProducerUtil.exampleFromFile
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortProducerUtil.validExampleAktivitetskortRecord
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.AktivitetskortBestilling
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.ArenaAktivitetskortBestilling
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.EksternAktivitetskortBestilling
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.KasseringsBestilling
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortStatus
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.*
import no.nav.veilarbaktivitet.aktivitetskort.feil.KeyErIkkeFunksjonellIdFeil
import no.nav.veilarbaktivitet.arena.model.ArenaId
import no.nav.veilarbaktivitet.person.Person
import no.nav.veilarbaktivitet.person.PersonService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.net.URL
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

internal class AktivitetsbestillingCreatorTest {
    var aktivitetsbestillingCreator: AktivitetsbestillingCreator? = null

    @BeforeEach
    fun setup() {
        val personService = Mockito.mock(PersonService::class.java)
        Mockito.`when`(
            personService.getAktorIdForPersonBruker(
                ArgumentMatchers.any(
                    Person::class.java
                )
            )
        ).thenReturn(Optional.of(Person.aktorId("12345678901")))
        aktivitetsbestillingCreator = AktivitetsbestillingCreator(personService)
    }

    @Test
    fun should_have_correct_timezone_when_serializing() {
        val jsonNode = validExampleAktivitetskortRecord(Person.fnr("1234567890"))
        val endretTidspunkt = jsonNode.path("aktivitetskort")["endretTidspunkt"].asText()
        Assertions.assertEquals("2022-01-01T00:00:00.001+01:00", endretTidspunkt)
    }

    @Test
    @SneakyThrows
    fun should_handle_zoned_datetime_format() {
        val json = exampleFromFile("validaktivitetskortZonedDatetime.json")
        val consumerRecord = ConsumerRecord("topic", 0, 0, "56155242-6481-43b5-9eac-4d7af695bf9d", json)
        val aktivitetskortBestilling =
            aktivitetsbestillingCreator!!.lagBestilling(consumerRecord, UUID.randomUUID()) as AktivitetskortBestilling
        val expected = ZonedDateTime.of(2022, 10, 19, 12, 0, 0, 99000000, ZoneId.of("UTC"))
        assertThat(aktivitetskortBestilling.aktivitetskort.endretTidspunkt)
            .isCloseTo(expected, within(1, ChronoUnit.MILLIS))
    }

    @Test
    @SneakyThrows
    fun should_handle_serialize_action_type() {
        val json = exampleFromFile("validaktivitetskortZonedDatetime.json")
        val consumerRecord = ConsumerRecord("topic", 0, 0, "56155242-6481-43b5-9eac-4d7af695bf9d", json)
        val aktivitetskortBestilling =
            aktivitetsbestillingCreator!!.lagBestilling(consumerRecord, UUID.randomUUID()) as AktivitetskortBestilling
        assertThat(aktivitetskortBestilling.messageId).isEqualTo(UUID.fromString("2edf9ba0-b195-49ff-a5cd-939c7f26826f"))
        assertThat(aktivitetskortBestilling.source).isEqualTo("TEAM_TILTAK")
        assertThat(aktivitetskortBestilling.actionType).isEqualTo(ActionType.UPSERT_AKTIVITETSKORT_V1)
    }

    @Test
    @SneakyThrows
    fun should_throw_exception_when_kafka_key_is_not_equal_to_funksjonell_id() {
        val json = exampleFromFile("validaktivitetskortZonedDatetime.json")
        val consumerRecord = ConsumerRecord("topic", 0, 0, "invalid-key", json)
        Assertions.assertThrows(KeyErIkkeFunksjonellIdFeil::class.java) {
            aktivitetsbestillingCreator!!.lagBestilling(
                consumerRecord,
                UUID.randomUUID()
            )
        }
    }

    @Test
    @SneakyThrows
    fun should_handle_zoned_datetime_format_pluss_time() {
        val json = exampleFromFile("validaktivitetskortZonedDatetime+Time.json")
        val consumerRecord = ConsumerRecord("topic", 0, 0, "56155242-6481-43b5-9eac-4d7af695bf9d", json)
        val aktivitetskortBestilling =
            aktivitetsbestillingCreator!!.lagBestilling(consumerRecord, UUID.randomUUID()) as AktivitetskortBestilling
        val expected = ZonedDateTime.of(2022, 10, 19, 11, 0, 0, 99000000, ZoneId.of("UTC"))
        assertThat(aktivitetskortBestilling.aktivitetskort.endretTidspunkt)
            .isCloseTo(expected, within(1, ChronoUnit.MILLIS))
    }

    @Test
    @SneakyThrows
    fun should_handle_UNzoned_datetime_format() {
        val json = exampleFromFile("validaktivitetskortUnzonedDatetime.json")
        val consumerRecord = ConsumerRecord("topic", 0, 0, "56155242-6481-43b5-9eac-4d7af695bf9d", json)
        val aktivitetskortBestilling =
            aktivitetsbestillingCreator!!.lagBestilling(consumerRecord, UUID.randomUUID()) as AktivitetskortBestilling
        val expected = ZonedDateTime.of(2022, 10, 19, 12, 0, 0, 0, ZoneId.of("Europe/Oslo"))
        val endretTidspunkt = aktivitetskortBestilling.aktivitetskort.endretTidspunkt
        assertThat(endretTidspunkt).isCloseTo(expected, within(1, ChronoUnit.MILLIS))
    }

    @Test
    @SneakyThrows
    fun should_be_able_to_deserialize_kasserings_bestilling() {
        val json = exampleFromFile("validkassering.json")
        val consumerRecord = ConsumerRecord("topic", 0, 0, "56155242-6481-43b5-9eac-4d7af695bf9d", json)
        val aktivitetskortBestilling = aktivitetsbestillingCreator!!.lagBestilling(consumerRecord, UUID.randomUUID())
        assertThat(aktivitetskortBestilling).isInstanceOf(
            KasseringsBestilling::class.java
        )
    }


    @Test
    @SneakyThrows
    fun should_handle_valid_aktivitetskort() {
        val json = exampleFromFile("validaktivitetskort.json")
        val consumerRecord = ConsumerRecord("topic", 0, 0, "56155242-6481-43b5-9eac-4d7af695bf9d", json)
        val aktivitetskortBestilling = aktivitetsbestillingCreator!!.lagBestilling(
            consumerRecord,
            UUID.randomUUID()
        ) as EksternAktivitetskortBestilling
        assertThat(aktivitetskortBestilling).isInstanceOf(
            AktivitetskortBestilling::class.java
        )
        assertThat(aktivitetskortBestilling.source).isEqualTo(MessageSource.TEAM_TILTAK.name)
        assertThat(aktivitetskortBestilling.aktivitetskortType).isEqualTo(AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD)
        val aktivitetskort = aktivitetskortBestilling.aktivitetskort
        assertThat(aktivitetskort.aktivitetStatus).isEqualTo(AktivitetskortStatus.PLANLAGT)
        assertThat(aktivitetskort.tittel).isEqualTo("The Elder Scrolls")
        assertThat(aktivitetskort.beskrivelse).isEqualTo("aktivitetsbeskrivelse")
        val oppgave = aktivitetskort.oppgave
        assertThat(oppgave!!.ekstern.url).isEqualTo(URL("http://localhost:8080/ekstern"))
        val handlinger = aktivitetskort.handlinger
        assertThat(handlinger).contains(LenkeSeksjon("tekst", "subtekst", URL("http://localhost:8080/ekstern"), LenkeType.EKSTERN))
        val etiketter = aktivitetskort.etiketter
        assertThat(etiketter).contains(Etikett("Etikett tekst", Sentiment.NEUTRAL, "INNSOKT"))
    }

    @Test
    @SneakyThrows
    fun should_handle_unknown_source_aktivitetskort() {
        val json = exampleFromFile("validaktivitetskortWithUnknownSource.json")
        val consumerRecord = ConsumerRecord("topic", 0, 0, "56155242-6481-43b5-9eac-4d7af695bf9d", json)
        val aktivitetskortBestilling = aktivitetsbestillingCreator!!.lagBestilling(
            consumerRecord,
            UUID.randomUUID()
        ) as EksternAktivitetskortBestilling
        assertThat(aktivitetskortBestilling).isInstanceOf(AktivitetskortBestilling::class.java)
        assertThat(aktivitetskortBestilling.source).isEqualTo("TEAM_UNKNOWN")
        assertThat(aktivitetskortBestilling.aktivitetskortType).isEqualTo(AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD)
        val aktivitetskort = aktivitetskortBestilling.aktivitetskort
        assertThat(aktivitetskort.aktivitetStatus).isEqualTo(AktivitetskortStatus.PLANLAGT)
        assertThat(aktivitetskort.tittel).isEqualTo("The Elder Scrolls")
        assertThat(aktivitetskort.beskrivelse).isEqualTo("aktivitetsbeskrivelse")
        val oppgave = aktivitetskort.oppgave
        assertThat(oppgave!!.ekstern.url).isEqualTo(URL("http://localhost:8080/ekstern"))
        val handlinger = aktivitetskort.handlinger
        assertThat(handlinger)
            .contains(LenkeSeksjon("tekst", "subtekst", URL("http://localhost:8080/ekstern"), LenkeType.EKSTERN))
    }

    @Test
    @SneakyThrows
    fun require_header_on_arena_tiltak() {
        val json = exampleFromFile("validArenaAktivitetskort.json")
        val consumerRecord = ConsumerRecord("topic", 0, 0L, "56155242-6481-43b5-9eac-4d7af695bf9d", json)
        val randomUUID = UUID.randomUUID()

        Assertions.assertThrows(RuntimeException::class.java) {
            aktivitetsbestillingCreator!!.lagBestilling(
                consumerRecord,
                randomUUID
            )
        }
        val arenaId = "ARENATA1234567"
        consumerRecord.headers().add(AktivitetsbestillingCreator.HEADER_EKSTERN_REFERANSE_ID, arenaId.toByteArray())
        Assertions.assertThrows(RuntimeException::class.java) {
            aktivitetsbestillingCreator!!.lagBestilling(
                consumerRecord,
                randomUUID
            )
        }
        val tiltakskode = "VASV"
        consumerRecord.headers()
            .add(AktivitetsbestillingCreator.HEADER_EKSTERN_ARENA_TILTAKSKODE, tiltakskode.toByteArray())
        Assertions.assertThrows(RuntimeException::class.java) {
            aktivitetsbestillingCreator!!.lagBestilling(
                consumerRecord,
                randomUUID
            )
        }
        val oppfolgingsperiode = "278f090f-09cc-4720-8638-f68020f3b417"
        consumerRecord.headers()
            .add(AktivitetsbestillingCreator.HEADER_OPPFOLGINGSPERIODE, oppfolgingsperiode.toByteArray())

        val aktivitetskortBestilling =
            aktivitetsbestillingCreator!!.lagBestilling(consumerRecord, randomUUID) as ArenaAktivitetskortBestilling
        assertThat(aktivitetskortBestilling).isInstanceOf(
            AktivitetskortBestilling::class.java
        )
        assertThat(aktivitetskortBestilling.source).isEqualTo(MessageSource.ARENA_TILTAK_AKTIVITET_ACL.name)
        assertThat(aktivitetskortBestilling.aktivitetskortType).isEqualTo(AktivitetskortType.ARENA_TILTAK)
        assertThat(aktivitetskortBestilling.oppfolgingsperiode).isEqualTo(UUID.fromString(oppfolgingsperiode))
        assertThat(aktivitetskortBestilling.eksternReferanseId).isEqualTo(ArenaId(arenaId))
        assertThat(aktivitetskortBestilling.arenaTiltakskode).isEqualTo(tiltakskode)
        val aktivitetskort = aktivitetskortBestilling.aktivitetskort
        assertThat(aktivitetskort.aktivitetStatus).isEqualTo(AktivitetskortStatus.PLANLAGT)
        assertThat(aktivitetskort.tittel).isEqualTo("The Elder Scrolls")
        assertThat(aktivitetskort.beskrivelse).isEqualTo("aktivitetsbeskrivelse")
    }

    @Test
    @SneakyThrows
    fun handle_oppfolgingsperiode_slutt_header() {
        val json = exampleFromFile("validArenaAktivitetskort.json")
        val consumerRecord = ConsumerRecord("topic", 0, 0L, "56155242-6481-43b5-9eac-4d7af695bf9d", json)

        val arenaId = "ARENATA1234567"
        consumerRecord.headers().add(AktivitetsbestillingCreator.HEADER_EKSTERN_REFERANSE_ID, arenaId.toByteArray())
        val tiltakskode = "VASV"
        consumerRecord.headers()
            .add(AktivitetsbestillingCreator.HEADER_EKSTERN_ARENA_TILTAKSKODE, tiltakskode.toByteArray())
        val oppfolgingsperiode = "278f090f-09cc-4720-8638-f68020f3b417"
        consumerRecord.headers()
            .add(AktivitetsbestillingCreator.HEADER_OPPFOLGINGSPERIODE, oppfolgingsperiode.toByteArray())
        val oppfolgingsperiodeSlutt = "2023-01-01T10:00:00.000Z"
        consumerRecord.headers()
            .add(AktivitetsbestillingCreator.HEADER_OPPFOLGINGSPERIODE_SLUTT, oppfolgingsperiodeSlutt.toByteArray())

        val aktivitetskortBestilling = aktivitetsbestillingCreator!!.lagBestilling(
            consumerRecord,
            UUID.randomUUID()
        ) as ArenaAktivitetskortBestilling
        assertThat(aktivitetskortBestilling).isInstanceOf(
            AktivitetskortBestilling::class.java
        )
        assertThat(aktivitetskortBestilling.source).isEqualTo(MessageSource.ARENA_TILTAK_AKTIVITET_ACL.name)
        assertThat(aktivitetskortBestilling.aktivitetskortType).isEqualTo(AktivitetskortType.ARENA_TILTAK)
        assertThat(aktivitetskortBestilling.oppfolgingsperiode).isEqualTo(UUID.fromString(oppfolgingsperiode))
        assertThat(aktivitetskortBestilling.eksternReferanseId).isEqualTo(ArenaId(arenaId))
        assertThat(aktivitetskortBestilling.oppfolgingsperiodeSlutt).isEqualTo(oppfolgingsperiodeSlutt)
        assertThat(aktivitetskortBestilling.arenaTiltakskode).isEqualTo(tiltakskode)
        val aktivitetskort = aktivitetskortBestilling.aktivitetskort
        assertThat(aktivitetskort.aktivitetStatus).isEqualTo(AktivitetskortStatus.PLANLAGT)
        assertThat(aktivitetskort.tittel).isEqualTo("The Elder Scrolls")
        assertThat(aktivitetskort.beskrivelse).isEqualTo("aktivitetsbeskrivelse")
    }
}
