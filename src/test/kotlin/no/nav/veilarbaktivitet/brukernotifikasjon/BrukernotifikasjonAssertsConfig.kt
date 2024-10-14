package no.nav.veilarbaktivitet.brukernotifikasjon

import lombok.Getter
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
    @Autowired val testService: KafkaTestService,
    @Value("\${topic.ut.brukernotifikasjon.brukervarsel}") val brukernotifikasjonBrukervarselTopic: String,
    @Value("\${topic.inn.brukernotifikasjon.brukervarselHendelse}")
    val brukernotifikasjonVarselHendelseTopic: String,
    @Autowired
    val kviteringsProducer: KafkaStringAvroTemplate<DoknotifikasjonStatus>,
    @Value("\${app.env.appname}")
    val appname: String,
    @Value("\${app.env.namespace}")
    val namespace: String,
    @Autowired
    val avsluttBrukernotifikasjonCron: AvsluttBrukernotifikasjonCron,
    @Autowired
    val sendBrukernotifikasjonCron: SendBrukernotifikasjonCron
) {

    fun createBrukerVarselConsumer(): Consumer<String, String> {
        return testService.createStringStringConsumer(brukernotifikasjonBrukervarselTopic)
    }

    fun createBrukernotifikasjonVarselHendelseConsumer(): Consumer<String, String> {
        return testService.createStringStringConsumer(brukernotifikasjonVarselHendelseTopic)
    }
}
