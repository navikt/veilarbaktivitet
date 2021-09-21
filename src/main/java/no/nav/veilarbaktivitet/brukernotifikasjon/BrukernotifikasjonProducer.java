package no.nav.veilarbaktivitet.brukernotifikasjon;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS;
import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;

@Configuration
class BrukernotifikasjonProducer {

    @Value("${spring.kafka.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    @Bean
    <T extends SpecificRecordBase> KafkaProducerClient<Nokkel, T> brukernotifiaksjonProducer(Properties onPremProducerProperties, MeterRegistry meterRegistry) {
        return KafkaProducerClientBuilder.<Nokkel, T>builder()
                .withMetrics(meterRegistry)
                .withProperties(onPremProducerProperties)
                .withAdditionalProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                        io.confluent.kafka.serializers.KafkaAvroSerializer.class)
                .withAdditionalProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                        io.confluent.kafka.serializers.KafkaAvroSerializer.class)
                .withAdditionalProperty(SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl)
                .withAdditionalProperty(AUTO_REGISTER_SCHEMAS, false)
                .build();
    }
}
