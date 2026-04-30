package no.nav.veilarbaktivitet.aktivitetskort

import java.io.IOException
import java.io.InputStreamReader
import java.io.UncheckedIOException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID
import lombok.SneakyThrows
import no.nav.common.json.JsonMapper
import no.nav.common.types.identer.NavIdent
import no.nav.common.types.identer.NorskIdent
import no.nav.veilarbaktivitet.aktivitet.domain.Ident
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.KasseringsBestilling
import no.nav.veilarbaktivitet.aktivitetskort.dto.Aktivitetskort
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortStatus
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType
import no.nav.veilarbaktivitet.aktivitetskort.dto.KafkaAktivitetskortWrapperDTO
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Attributt
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Etikett
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.LenkeSeksjon
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.LenkeType
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.MessageSource
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Oppgave
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Oppgaver
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Sentiment
import no.nav.veilarbaktivitet.person.Innsender
import no.nav.veilarbaktivitet.person.Person
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.util.FileCopyUtils
import tools.jackson.databind.JsonNode
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode
import tools.jackson.databind.json.JsonMapper as JacksonJsonMapper

object AktivitetskortProducerUtil {
    private val objectMapper = (JsonMapper.defaultObjectMapper() as JacksonJsonMapper)
    .rebuild()
    .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
    .build()

    fun readFileToString(path: String): String {
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
        val kafkaAktivitetskortWrapperDTO = kafkaArenaAktivitetWrapper(fnr)
        return aktivitetMessageNode(kafkaAktivitetskortWrapperDTO)
    }

    @JvmStatic
    fun validExampleKasseringsRecord(): JsonNode {
        val kasseringsBestilling = KasseringsBestilling(
            MessageSource.TEAM_TILTAK.name,
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
        val kafkaAktivitetskortWrapperDTO = kafkaArenaAktivitetWrapper(fnr)
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
    fun missingFieldRecord(fnr: Person.Fnr): Pair<String, UUID?> {
        val kafkaAktivitetskortWrapperDTO = kafkaArenaAktivitetWrapper(fnr)
        val jsonNode = aktivitetMessageNode(kafkaAktivitetskortWrapperDTO)
        val payload = jsonNode.path("aktivitetskort") as ObjectNode
        payload.remove("tittel")
        return Pair(jsonNode.toString(), kafkaAktivitetskortWrapperDTO.messageId)
    }

    @JvmStatic
    fun extraFieldRecord(fnr: Person.Fnr): Pair<String, UUID?> {
        val kafkaAktivitetskortWrapperDTO = kafkaArenaAktivitetWrapper(fnr)
        val jsonNode = aktivitetMessageNode(kafkaAktivitetskortWrapperDTO)
        val payload = jsonNode.path("aktivitetskort") as ObjectNode
        payload.put("kake", "123")
        return Pair(jsonNode.toString(), kafkaAktivitetskortWrapperDTO.messageId)
    }

    @JvmStatic
    fun invalidDateFieldRecord(fnr: Person.Fnr): Pair<String, UUID?> {
        val kafkaAktivitetskortWrapperDTO = kafkaArenaAktivitetWrapper(fnr)
        val jsonNode = aktivitetMessageNode(kafkaAktivitetskortWrapperDTO)
        val payload = jsonNode.path("aktivitetskort") as ObjectNode
        payload.set("startDato", StringNode("2022/-1/04T12:00:00+02:00"))
        return Pair(jsonNode.toString(), kafkaAktivitetskortWrapperDTO.messageId)
    }

    @JvmStatic
    @SneakyThrows
    fun kafkaArenaAktivitetWrapper(fnr: Person.Fnr): KafkaAktivitetskortWrapperDTO {
        val aktivitetskort = Aktivitetskort(
            id = UUID.randomUUID(),
            personIdent = fnr.get(),
            startDato = LocalDate.now().minusDays(30),
            sluttDato = LocalDate.now().minusDays(30),
            tittel = "The Elder Scrolls: Arena",
            beskrivelse = "arenabeskrivelse",
            aktivitetStatus = AktivitetskortStatus.PLANLAGT,
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
                Etikett("Innsøkt", Sentiment.NEUTRAL, "INNSOKT"),
                Etikett("Utsøkt", Sentiment.NEGATIVE, "UTSOKT")
            ),
            avtaltMedNav = false
        )
        return KafkaAktivitetskortWrapperDTO(
            messageId = UUID.randomUUID(),
            source = MessageSource.ARENA_TILTAK_AKTIVITET_ACL.name,
            aktivitetskort = aktivitetskort,
            aktivitetskortType = AktivitetskortType.ARENA_TILTAK)
    }
}
