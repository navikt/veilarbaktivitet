package no.nav.veilarbaktivitet.brukernotifikasjon

import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus
import no.nav.veilarbaktivitet.brukernotifikasjon.avslutt.AvsluttBrukernotifikasjonCron
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.SendBrukernotifikasjonCron
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringAvroTemplate
import no.nav.veilarbaktivitet.util.KafkaTestService
import org.apache.kafka.clients.consumer.Consumer
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
internal class BrukernotifikasjonAssertsConfig(
    val testService: KafkaTestService,
    @Value("\${topic.inn.eksternVarselKvittering}")
    val kviteringsToppic: String,
    val avsluttBrukernotifikasjonCron: AvsluttBrukernotifikasjonCron,
    val sendBrukernotifikasjonCron: SendBrukernotifikasjonCron,
    var kviteringsProducer: KafkaStringAvroTemplate<DoknotifikasjonStatus>,
    @Value("\${topic.ut.brukernotifikasjon}")
    val brukernotifikasjonTopic: String,
) {
    fun createBrukernotifikasjonConsumer(): Consumer<String, String> {
        return testService.createStringStringConsumer(brukernotifikasjonTopic)
    }


    fun createOppgaveConsumer(): Consumer<NokkelInput, OppgaveInput> {
        return testService.createAvroAvroConsumer(oppgaveTopic)
    }

    fun createBeskjedConsumer(): Consumer<NokkelInput, BeskjedInput> {
        return testService.createAvroAvroConsumer(beskjedTopic)
    }

    fun createDoneConsumer(): Consumer<NokkelInput, DoneInput> {
        return testService.createAvroAvroConsumer(brukernotifkasjonFerdigTopic)
    }
}
