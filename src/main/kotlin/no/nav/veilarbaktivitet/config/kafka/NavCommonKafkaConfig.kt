package no.nav.veilarbaktivitet.config.kafka

import io.getunleash.Unleash
import io.micrometer.core.instrument.MeterRegistry
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder
import no.nav.common.kafka.util.KafkaPropertiesPreset
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetsKortConsumerConfig
import no.nav.veilarbaktivitet.kvp.KvpAvsluttetConsumerConfig
import no.nav.veilarbaktivitet.kvp.KvpAvsluttetKafkaDTO
import no.nav.veilarbaktivitet.oppfolging.periode.OppfolgingsperiodeConsumerConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.util.*

@Configuration
open class NavCommonKafkaConfig {

    private val kafkaEnabled = System.getenv("KAFKA_ENABLED")?.toBoolean() ?: false

    init {
        val logger = LoggerFactory.getLogger(this::class.java)
        logger.info("Kafka enabled: $kafkaEnabled")
    }

    @Bean
    open fun aktivitetskortConsumerClient(
        topicConfig: AktivitetsKortConsumerConfig,
        meterRegistry: MeterRegistry,
        aktivitetskortConsumerProperties: Properties,
        unleash: Unleash
    ): KafkaConsumerClient {
        return KafkaConsumerClientBuilder.builder()
            .withProperties(aktivitetskortConsumerProperties)
            .withToggle { if (kafkaEnabled) false else unleash.isEnabled(AKTIVITETSKORT_KAFKACONSUMER_DISABLED) }
            .withTopicConfig(
                KafkaConsumerClientBuilder.TopicConfig<String, String>()
                    .withConsumerConfig(topicConfig)
                    .withMetrics(meterRegistry)
                    .withLogging()
            ).build()
    }

    @Bean
    open fun oppfolgingsperiodeConsumerClient(
        topicConfig: OppfolgingsperiodeConsumerConfig,
        meterRegistry: MeterRegistry,
        consumerProperties: Properties,
        unleash: Unleash
    ): KafkaConsumerClient {
        return KafkaConsumerClientBuilder.builder()
            .withProperties(consumerProperties)
            .withToggle { if(kafkaEnabled) false else unleash.isEnabled(OPPFOLGINGSPERIODE_KAFKACONSUMER_DISABLED) }
            .withTopicConfig(
                KafkaConsumerClientBuilder.TopicConfig<String, String>()
                    .withConsumerConfig(topicConfig)
                    .withMetrics(meterRegistry)
                    .withLogging()
            ).build()
    }

    @Bean
    open fun kvpAvsluttetConsumerClient(
        topicConfig: KvpAvsluttetConsumerConfig,
        meterRegistry: MeterRegistry,
        consumerProperties: Properties,
        unleash: Unleash
    ): KafkaConsumerClient {
        return KafkaConsumerClientBuilder.builder()
            .withProperties(consumerProperties)
            .withToggle { if(kafkaEnabled) false else unleash.isEnabled(KVPAVSLUTTET_KAFKACONSUMER_DISABLED) }
            .withTopicConfig(
                KafkaConsumerClientBuilder.TopicConfig<String, KvpAvsluttetKafkaDTO>()
                    .withConsumerConfig(topicConfig)
                    .withMetrics(meterRegistry)
                    .withLogging()
            ).build()
    }

    @Bean
    open fun consumerClients(kafkaConsumerClients: List<KafkaConsumerClient>): List<KafkaConsumerClient> {
        return kafkaConsumerClients
            .onEach { it.start() }
    }

    @Bean
    open fun aivenProducerClient(
        aivenProducerProperties: Properties,
        meterRegistry: MeterRegistry
    ): KafkaProducerClient<String, String> {
        return KafkaProducerClientBuilder.builder<String, String>()
            .withMetrics(meterRegistry)
            .withProperties(aivenProducerProperties)
            .build()
    }

    @Bean
    @Profile("!test")
    open fun aktivitetskortConsumerProperties(): Properties = KafkaPropertiesPreset.aivenDefaultConsumerProperties(AKTIVITETSKORT_CONSUMER_GROUP)

    @Bean
    @Profile("!test")
    open fun consumerProperties(@Value("\${app.kafka.consumer-group-id}") consumerGroupId: String): Properties
        = KafkaPropertiesPreset.aivenDefaultConsumerProperties(consumerGroupId)

    @Bean
    @Profile("!test")
    open fun aivenProducerProperties(): Properties = KafkaPropertiesPreset.aivenDefaultProducerProperties(PRODUCER_CLIENT_ID)

    companion object {
        const val CONSUMER_GROUP_ID: String = "veilarbaktivitet-consumer"
        const val AKTIVITETSKORT_CONSUMER_GROUP = "veilarbaktivitet-aktivitetskort-consumer"
        const val PRODUCER_CLIENT_ID = "veilarbaktivitet-producer"
        private const val AKTIVITETSKORT_KAFKACONSUMER_DISABLED =
            "veilarbaktivitet.kafka.aktivitetskort.aiven.consumer.disabled"
        private const val KVPAVSLUTTET_KAFKACONSUMER_DISABLED =
            "veilarbaktivitet.kafka.kvpavsluttet.aiven.consumer.disabled"
        private const val OPPFOLGINGSPERIODE_KAFKACONSUMER_DISABLED =
            "veilarbaktivitet.kafka.oppfolginsperiode.aiven.consumer.disabled"
    }
}
