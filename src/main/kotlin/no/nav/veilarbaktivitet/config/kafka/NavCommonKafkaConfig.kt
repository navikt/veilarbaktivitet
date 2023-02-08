package no.nav.veilarbaktivitet.config.kafka

import io.micrometer.core.instrument.MeterRegistry
import no.nav.common.featuretoggle.UnleashClient
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder
import no.nav.common.kafka.util.KafkaPropertiesPreset
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetsKortConsumerConfig
import no.nav.veilarbaktivitet.kvp.KvpAvsluttetConsumerConfig
import no.nav.veilarbaktivitet.kvp.KvpAvsluttetDTO
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.util.*

@Configuration
open class NavCommonKafkaConfig(
    private val meterRegistry: MeterRegistry,
    private val unleashClient: UnleashClient,
) {

    companion object {
        private const val AKTIVITETSKORT_KAFKACONSUMER_DISABLED = "veilarbaktivitet.kafka.aktivitetskort.aiven.consumer.disabled"
        private const val KVPAVSLUTTET_KAFKACONSUMER_DISABLED = "veilarbaktivitet.kafka.kvpavsluttet.aiven.consumer.disabled"
    }

    @Bean
    open fun aktivitetskortConsumerClient(consumerConfig: AktivitetsKortConsumerConfig, consumerProperties: Properties): KafkaConsumerClientBuilder {
        return KafkaConsumerClientBuilder.builder()
            .withProperties(consumerProperties)
            .withToggle { unleashClient.isEnabled(AKTIVITETSKORT_KAFKACONSUMER_DISABLED) }
            .withTopicConfig(
                KafkaConsumerClientBuilder.TopicConfig<String, String>()
                    .withConsumerConfig(consumerConfig)
                    .withMetrics(meterRegistry)
                    .withLogging()
            )
    }

    @Bean
    open fun kvpAvsluttetConsumerClient(consumerConfig: KvpAvsluttetConsumerConfig, consumerProperties: Properties): KafkaConsumerClientBuilder {
        return KafkaConsumerClientBuilder.builder()
            .withProperties(consumerProperties)
            .withToggle { unleashClient.isEnabled(KVPAVSLUTTET_KAFKACONSUMER_DISABLED) }
            .withTopicConfig(
                KafkaConsumerClientBuilder.TopicConfig<String, KvpAvsluttetDTO>()
                    .withConsumerConfig(consumerConfig)
                    .withMetrics(meterRegistry)
                    .withLogging()
            )
    }

    @Bean
    open fun consumerClients(kafkaConsumerClientBuilders: List<KafkaConsumerClientBuilder>): List<KafkaConsumerClient> {
        return kafkaConsumerClientBuilders
            .map { it.build() }
            .onEach { it.start() }
    }

    @Bean
    open fun producerClient(producerProperties: Properties): KafkaProducerClient<String, String> {
        return KafkaProducerClientBuilder.builder<String, String>()
            .withMetrics(meterRegistry)
            .withProperties(producerProperties)
            .build()
    }

    @Bean
    @Profile("!dev")
    open fun consumerProperties(@Value("\${app.kafka.consumer-group-id}") consumerGroupId: String): Properties {
        return KafkaPropertiesPreset.aivenDefaultConsumerProperties(consumerGroupId)
    }

    @Bean
    @Profile("!dev")
    open fun producerProperties(@Value("\${app.kafka.producer-client-id}") producerClientId: String): Properties {
        return KafkaPropertiesPreset.aivenDefaultProducerProperties(producerClientId)
    }

}
