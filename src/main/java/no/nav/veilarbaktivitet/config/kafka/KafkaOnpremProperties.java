package no.nav.veilarbaktivitet.config.kafka;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaOnpremProperties {
    String brokersUrl;
    String endringPaaAktivitetTopic;
    String oppfolgingAvsluttetTopic;
    String kvpAvsluttetTopic;
}
