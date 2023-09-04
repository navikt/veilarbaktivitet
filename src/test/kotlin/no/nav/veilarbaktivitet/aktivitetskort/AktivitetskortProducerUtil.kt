package no.nav.veilarbaktivitet.aktivitetskort

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import lombok.SneakyThrows
import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.NavIdent
import no.nav.common.types.identer.NorskIdent
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.domain.Ident
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.KasseringsBestilling
import no.nav.veilarbaktivitet.aktivitetskort.dto.KafkaAktivitetskortWrapperDTO
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.*
import no.nav.veilarbaktivitet.person.Innsender
import no.nav.veilarbaktivitet.person.Person
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.util.FileCopyUtils
import java.io.IOException
import java.io.InputStreamReader
import java.io.UncheckedIOException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

object AktivitetskortProducerUtil {
    private val objectMapper = JsonUtils.getMapper().copy()
        .registerModule(JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

    fun readFileToString(path: String?): String {
        val resourceLoader: ResourceLoader = DefaultResourceLoader()
        val resource = resourceLoader.getResource(path)
        return asString(resource)
    }

    fun asString(resource: Resource): String {
        try {
            InputStreamReader(
                resource.inputStream,
                StandardCharsets.UTF_8
            ).use { reader -> return FileCopyUtils.copyToString(reader) }
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
    }

    private fun aktivitetMessageNode(kafkaAktivitetskortWrapperDTO: KafkaAktivitetskortWrapperDTO): JsonNode {
        return objectMapper
            .valueToTree(kafkaAktivitetskortWrapperDTO)
    }

    private fun kasserMessageNode(kasseringsBestilling: KasseringsBestilling): JsonNode {
        return objectMapper
            .valueToTree(kasseringsBestilling)
    }

    @JvmStatic
    fun validExampleAktivitetskortRecord(fnr: Person.Fnr): JsonNode {
        val kafkaAktivitetskortWrapperDTO = kafkaAktivitetWrapper(fnr)
        return aktivitetMessageNode(kafkaAktivitetskortWrapperDTO)
    }

    @JvmStatic
    fun validExampleKasseringsRecord(): JsonNode {
        val kasseringsBestilling = KasseringsBestilling(
            "team-tiltak",
            UUID.randomUUID(),
//            ActionType.KASSER_AKTIVITET,
            NavIdent.of("z123456"),
            NorskIdent.of("12121212121"),
            UUID.randomUUID(),
            "Fordi"
        )
        return kasserMessageNode(kasseringsBestilling)
    }

    @JvmStatic
    fun invalidExampleRecord(fnr: Person.Fnr): JsonNode {
        val kafkaAktivitetskortWrapperDTO = kafkaAktivitetWrapper(fnr)
        val jsonNode = aktivitetMessageNode(kafkaAktivitetskortWrapperDTO)
        val objectNode = jsonNode as ObjectNode
        objectNode.remove("aktivitetskortType")
        return jsonNode
    }

    @JvmStatic
    @SneakyThrows
    fun exampleFromFile(filename: String?): String {
        return readFileToString("__files/aktivitetskort/%s".format(filename))
    }

    @JvmStatic
    fun missingFieldRecord(fnr: Person.Fnr): kotlin.Pair<String, UUID?> {
        val kafkaAktivitetskortWrapperDTO = kafkaAktivitetWrapper(fnr)
        val jsonNode = aktivitetMessageNode(kafkaAktivitetskortWrapperDTO)
        val payload = jsonNode.path("aktivitetskort") as ObjectNode
        payload.remove("tittel")
        return Pair(jsonNode.toString(), kafkaAktivitetskortWrapperDTO.messageId)
    }

    @JvmStatic
    fun extraFieldRecord(fnr: Person.Fnr): kotlin.Pair<String, UUID?> {
        val kafkaAktivitetskortWrapperDTO = kafkaAktivitetWrapper(fnr)
        val jsonNode = aktivitetMessageNode(kafkaAktivitetskortWrapperDTO)
        val payload = jsonNode.path("aktivitetskort") as ObjectNode
        payload.put("kake", "123")
        return Pair(jsonNode.toString(), kafkaAktivitetskortWrapperDTO.messageId)
    }

    @JvmStatic
    fun invalidDateFieldRecord(fnr: Person.Fnr): kotlin.Pair<String, UUID?> {
        val kafkaAktivitetskortWrapperDTO = kafkaAktivitetWrapper(fnr)
        val jsonNode = aktivitetMessageNode(kafkaAktivitetskortWrapperDTO)
        val payload = jsonNode.path("aktivitetskort") as ObjectNode
        payload.set<JsonNode>("startDato", TextNode("2022/-1/04T12:00:00+02:00"))
        return Pair(jsonNode.toString(), kafkaAktivitetskortWrapperDTO.messageId)
    }

    @JvmStatic
    @SneakyThrows
    fun kafkaAktivitetWrapper(fnr: Person.Fnr): KafkaAktivitetskortWrapperDTO {
        val aktivitetskort = Aktivitetskort(
            id = UUID.randomUUID(),
            personIdent = fnr.get(),
            startDato = LocalDate.now().minusDays(30),
            sluttDato = LocalDate.now().minusDays(30),
            tittel = "The Elder Scrolls: Arena",
            beskrivelse = "arenabeskrivelse",
            aktivitetStatus = AktivitetStatus.PLANLAGT,
            endretAv = Ident("arenaEndretav", Innsender.ARENAIDENT),
            endretTidspunkt = ZonedDateTime.of(2022, 1, 1, 0, 0, 0, 1000000, ZoneOffset.ofHours(1)),
            detaljer = listOf(
                Attributt(
                    "deltakelsesprosent",
                    "40"
                ),
                Attributt(
                    "dagerPerUke",
                    "2"
                ),
            ),
            oppgave = Oppgaver(
                Oppgave(
                    "tekst",
                    "subtekst",
                    URL("http://localhost:8080/ekstern")
                ),
                null
            ),
            handlinger = listOf(
                LenkeSeksjon(
                    "tekst",
                    "subtekst",
                    URL("http://localhost:8080/ekstern"),
                    LenkeType.EKSTERN
                ),
                LenkeSeksjon(
                    "tekst",
                    "subtekst",
                    URL("http://localhost:8080/intern"),
                    LenkeType.INTERN
                )
            ),
            etiketter = listOf(
                Etikett("INNSOKT"),
                Etikett("UTSOKT")
            ),
            avtaltMedNav = false
        )
        return KafkaAktivitetskortWrapperDTO(
            messageId = UUID.randomUUID(),
            source = AktivitetsbestillingCreator.ARENA_TILTAK_AKTIVITET_ACL,
            aktivitetskort = aktivitetskort,
            aktivitetskortType = AktivitetskortType.ARENA_TILTAK)
    }

    @JvmRecord
    data class Pair(@JvmField val json: String, @JvmField val messageId: UUID)
}
