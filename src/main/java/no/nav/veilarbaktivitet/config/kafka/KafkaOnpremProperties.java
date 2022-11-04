package no.nav.veilarbaktivitet.config.kafka;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaOnpremProperties {
    String brokersUrl;
    String producerClientId;
    String consumerGroupId;
}
