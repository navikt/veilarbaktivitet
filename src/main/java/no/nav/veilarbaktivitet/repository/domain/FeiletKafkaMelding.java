package no.nav.veilarbaktivitet.repository.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.veilarbaktivitet.kafka.KafkaTopics;

@Data
@Accessors(chain = true)
public class FeiletKafkaMelding {
    long id;
    KafkaTopics.Topic topic;
    String key;
    String jsonPayload;
    long offset;
    MeldingType type;
}
