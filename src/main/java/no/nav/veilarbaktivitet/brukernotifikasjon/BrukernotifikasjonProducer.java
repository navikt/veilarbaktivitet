package no.nav.veilarbaktivitet.brukernotifikasjon;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.Oppgave;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;

@Configuration
class BrukernotifikasjonProducer {

    @Value("${spring.kafka.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    @Bean
    KafkaProducerClient<Nokkel, Oppgave> brukernotifiaksjonOppgaveProducer(Properties onPremProducerProperties, MeterRegistry meterRegistry) {
        return KafkaProducerClientBuilder.<Nokkel, Oppgave>builder()
                .withMetrics(meterRegistry)
                .withProperties(onPremProducerProperties)
                .withAdditionalProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                        io.confluent.kafka.serializers.KafkaAvroSerializer.class)
                .withAdditionalProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                        io.confluent.kafka.serializers.KafkaAvroSerializer.class)
                .withAdditionalProperty(SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl)
                .build();
    }
}
