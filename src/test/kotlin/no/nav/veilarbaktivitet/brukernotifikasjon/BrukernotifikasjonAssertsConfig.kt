package no.nav.veilarbaktivitet.brukernotifikasjon

import lombok.Getter
import no.nav.common.json.JsonUtils
import no.nav.veilarbaktivitet.brukernotifikasjon.avslutt.AvsluttBrukernotifikasjonCron
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.InaktiverVarselDto
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.MinSideBrukernotifikasjonsId
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.SendBrukernotifikasjonCron
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.EksternVarselHendelseDTO
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringTemplate
import no.nav.veilarbaktivitet.util.KafkaTestService
import org.apache.kafka.clients.consumer.Consumer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.support.SendResult
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.stereotype.Service

@Service
@Getter
class BrukernotifikasjonAssertsConfig(
    @Autowired val testService: KafkaTestService,
    @Value("\${topic.ut.brukernotifikasjon.brukervarsel}")
    val brukernotifikasjonBrukervarselTopic: String,
    @Value("\${topic.inn.brukernotifikasjon.brukervarselHendelse}")
    val brukernotifikasjonVarselHendelseTopic: String,
    @Autowired
    private val brukernotifikasjonVarselHendelseProducer: KafkaStringTemplate,
    @Value("\${app.env.appname}")
    val appname: String,
    @Value("\${app.env.namespace}")
    val namespace: String,
    @Autowired
    val avsluttBrukernotifikasjonCron: AvsluttBrukernotifikasjonCron,
    @Autowired
    val sendBrukernotifikasjonCron: SendBrukernotifikasjonCron
) {

    fun createBrukerVarselConsumer(): TestConsumer {
        return TestConsumer(
            testService.createStringStringConsumer(brukernotifikasjonBrukervarselTopic),
            brukernotifikasjonBrukervarselTopic
        )
    }

    fun brukernotifikasjonVarselProducer(): TestProducer {
        return TestProducer(
            brukernotifikasjonVarselHendelseProducer,
            brukernotifikasjonVarselHendelseTopic
        )
    }
}

class TestConsumer(
    val consumer: Consumer<String, String>,
    val topic: String
) {
    private fun getSingleRecord (): String {
        return KafkaTestUtils.getSingleRecord(
            consumer,
            topic,
            KafkaTestService.DEFAULT_WAIT_TIMEOUT_DURATION
        ).value()
    }
    fun waitForOpprettVarsel(): OpprettVarselDto {
        return JsonUtils.fromJson(getSingleRecord(), OpprettVarselDto::class.java)
    }
    fun waitForInaktiverVarsel(): InaktiverVarselDto {
        return JsonUtils.fromJson(getSingleRecord(), InaktiverVarselDto::class.java)
    }
}

class TestProducer(
    val brukernotifikasjonVarselHendelseProducer: KafkaStringTemplate,
    val topic: String) {

    fun publiserBrukernotifikasjonVarselHendelse(varselId: MinSideBrukernotifikasjonsId, eksternVarselHendelseDTO: EksternVarselHendelseDTO): SendResult<String, String> {
        return brukernotifikasjonVarselHendelseProducer.send(
            topic, varselId.value.toString(), JsonUtils.toJson(eksternVarselHendelseDTO)
        ).get()
    }

}