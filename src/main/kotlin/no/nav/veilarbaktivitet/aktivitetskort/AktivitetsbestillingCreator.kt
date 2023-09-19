package no.nav.veilarbaktivitet.aktivitetskort

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.common.client.aktorregister.IngenGjeldendeIdentException
import no.nav.common.json.JsonMapper
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.ArenaAktivitetskortBestilling
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.EksternAktivitetskortBestilling
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType
import no.nav.veilarbaktivitet.aktivitetskort.dto.BestillingBase
import no.nav.veilarbaktivitet.aktivitetskort.dto.KafkaAktivitetskortWrapperDTO
import no.nav.veilarbaktivitet.aktivitetskort.feil.DeserialiseringsFeil
import no.nav.veilarbaktivitet.aktivitetskort.feil.ErrorMessage
import no.nav.veilarbaktivitet.aktivitetskort.feil.KeyErIkkeFunksjonellIdFeil
import no.nav.veilarbaktivitet.aktivitetskort.feil.UgyldigIdentFeil
import no.nav.veilarbaktivitet.arena.model.ArenaId
import no.nav.veilarbaktivitet.person.Person
import no.nav.veilarbaktivitet.person.PersonService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Component
import java.util.*

@Component
class AktivitetsbestillingCreator (
    private val personService: PersonService
) {

    @Throws(UgyldigIdentFeil::class)
    private fun hentAktorId(fnr: Person.Fnr): Person.AktorId {
        return try {
            personService.getAktorIdForPersonBruker(fnr)
                .orElseThrow { UgyldigIdentFeil("Ugyldig identtype for " + fnr.get(), null) }
        } catch (e: IngenGjeldendeIdentException) {
            throw UgyldigIdentFeil("Ident ikke funnet eller ugyldig ident for fnr :" + fnr.get(), e)
        }
    }

    @Throws(DeserialiseringsFeil::class, UgyldigIdentFeil::class, KeyErIkkeFunksjonellIdFeil::class)
    fun lagBestilling(consumerRecord: ConsumerRecord<String, String>, messageId: UUID?): BestillingBase {
        val melding = deserialiser(consumerRecord)
        val resolvedMessageId = (melding.messageId ?: messageId) ?: throw RuntimeException("Mangler påkrevet messageId på aktivitetskort melding")
        if (melding.getAktivitetskortId().toString() != consumerRecord.key()) throw KeyErIkkeFunksjonellIdFeil(
            ErrorMessage(
                String.format(
                    "aktivitetsId: %s må være lik kafka-meldings-id: %s",
                    melding.getAktivitetskortId(),
                    consumerRecord.key()
                )
            ), null
        )
        return if (melding is KafkaAktivitetskortWrapperDTO) {
            val aktorId = hentAktorId(Person.fnr(melding.aktivitetskort.personIdent))
            val erArenaAktivitet = AktivitetskortType.ARENA_TILTAK == melding.aktivitetskortType
            if (erArenaAktivitet) {
                ArenaAktivitetskortBestilling(
                    melding.aktivitetskort,
                    melding.source,
                    melding.aktivitetskortType,
                    getEksternReferanseId(consumerRecord),
                    getArenaTiltakskode(consumerRecord),
                    resolvedMessageId,
                    melding.actionType,
                    aktorId
                )
            } else {
                EksternAktivitetskortBestilling(
                    melding.aktivitetskort,
                    melding.source,
                    melding.aktivitetskortType,
                    resolvedMessageId,
                    melding.actionType,
                    aktorId
                )
            }
        } else {
            melding
        }
    }

    companion object {
        const val HEADER_EKSTERN_REFERANSE_ID = "eksternReferanseId"
        const val HEADER_EKSTERN_ARENA_TILTAKSKODE = "arenaTiltakskode"
        const val ARENA_TILTAK_AKTIVITET_ACL = "ARENA_TILTAK_AKTIVITET_ACL"
        private var objectMapper: ObjectMapper? = null
        private val mapper: ObjectMapper?
            get() {
                if (objectMapper != null) return objectMapper
                objectMapper = JsonMapper.defaultObjectMapper()
                    .registerKotlinModule()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                return objectMapper
            }

        @Throws(DeserialiseringsFeil::class)
        fun deserialiser(consumerRecord: ConsumerRecord<String, String>): BestillingBase {
            return try {
                mapper!!.readValue(consumerRecord.value(), BestillingBase::class.java)
            } catch (e: Exception) {
                throw DeserialiseringsFeil(
                    ErrorMessage(e.message),
                    e
                )
            }
        }

        private fun getEksternReferanseId(consumerRecord: ConsumerRecord<String, String>): ArenaId {
            val header = consumerRecord.headers().lastHeader(HEADER_EKSTERN_REFERANSE_ID)
                ?: throw RuntimeException("Mangler Arena Header for ArenaTiltak aktivitetskort")
            val eksternReferanseIdBytes = header.value()
            return ArenaId(String(eksternReferanseIdBytes))
        }

        private fun getArenaTiltakskode(consumerRecord: ConsumerRecord<String, String>): String {
            val header = consumerRecord.headers().lastHeader(HEADER_EKSTERN_ARENA_TILTAKSKODE)
                ?: throw RuntimeException("Mangler Arena Header for ArenaTiltak aktivitetskort")
            val arenaTiltakskode = header.value()
            return String(arenaTiltakskode)
        }
    }
}
