package no.nav.veilarbaktivitet.brukernotifikasjon

import lombok.Getter
import no.nav.common.json.JsonUtils
import no.nav.veilarbaktivitet.brukernotifikasjon.avslutt.AvsluttBrukernotifikasjonCron
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.InaktiverVarselDto
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.MinSideVarselId
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.SendMinsideVarselFraOutboxCron
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.VarselHendelse
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
    val sendMinsideVarselFraOutboxCron: SendMinsideVarselFraOutboxCron
) {

    fun createBrukerVarselConsumer(kafkaTestService: KafkaTestService): TestConsumer {
        return TestConsumer(
            testService.createStringStringConsumer(brukernotifikasjonBrukervarselTopic),
            brukernotifikasjonBrukervarselTopic,
            kafkaTestService
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
    val topic: String,
    val kafkaTestService: KafkaTestService
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
    fun harKonsumertAlleMeldinger(): Boolean {
        return kafkaTestService.harKonsumertAlleMeldinger(
            topic,
            consumer,
        )
    }
}

class TestProducer(
    val brukernotifikasjonVarselHendelseProducer: KafkaStringTemplate,
    val topic: String) {

    fun publiserBrukernotifikasjonVarselHendelse(varselId: MinSideVarselId, varselHendelse: VarselHendelse): SendResult<String, String> {
        return brukernotifikasjonVarselHendelseProducer.send(
            topic, varselId.value.toString(), JsonUtils.toJson(varselHendelse)
        ).get()
    }

}