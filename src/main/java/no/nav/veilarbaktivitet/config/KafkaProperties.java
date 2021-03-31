package no.nav.veilarbaktivitet.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaProperties {
    String brokersUrl;
    String endringPaaAktivitetTopic;
    String oppfolgingAvsluttetTopic;
    String kvpAvsluttetTopic;
}
