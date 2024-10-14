package no.nav.veilarbaktivitet.brukernotifikasjon

import lombok.Getter
import no.nav.brukernotifikasjon.schemas.input.DoneInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus
import no.nav.veilarbaktivitet.brukernotifikasjon.avslutt.AvsluttBrukernotifikasjonCron
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.SendBrukernotifikasjonCron
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringAvroTemplate
import no.nav.veilarbaktivitet.util.KafkaTestService
import org.apache.kafka.clients.consumer.Consumer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
@Getter
class BrukernotifikasjonAssertsConfig(
    @Autowired
    private val testService: KafkaTestService,
    @Value("\${topic.ut.brukernotifikasjon.brukervarsel}")
    private val brukernotifikasjonBrukervarselTopic: String,
    @Value("\${topic.ut.brukernotifikasjon.done}")
    private val brukernotifkasjonFerdigTopic: String,
    @Value("\${topic.inn.eksternVarselKvittering}")
    private val kviteringsToppic: String,
    @Autowired
    private val kviteringsProducer: KafkaStringAvroTemplate<DoknotifikasjonStatus>,
    @Value("\${app.env.appname}")
    private val appname: String,
    @Value("\${app.env.namespace}")
    private val namespace: String,
    @Autowired
    private val avsluttBrukernotifikasjonCron: AvsluttBrukernotifikasjonCron,
    @Autowired
    private val sendBrukernotifikasjonCron: SendBrukernotifikasjonCron
) {

    fun createBrukerVarselConsumer(): Consumer<String, String> {
        return testService.createStringStringConsumer(brukernotifikasjonBrukervarselTopic)
    }

    fun createDoneConsumer(): Consumer<NokkelInput, DoneInput> {
        return testService.createAvroAvroConsumer(brukernotifkasjonFerdigTopic)
    }
}
