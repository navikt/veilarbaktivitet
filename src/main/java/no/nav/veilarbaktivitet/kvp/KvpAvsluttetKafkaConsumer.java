package no.nav.veilarbaktivitet.kvp;

import io.micrometer.core.annotation.Timed;
import lombok.EqualsAndHashCode;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import no.nav.veilarbaktivitet.person.Person;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;


@Service
@EqualsAndHashCode(callSuper = false)
public class KvpAvsluttetKafkaConsumer extends no.nav.common.kafka.consumer.util.TopicConsumerConfig<String, KvpAvsluttetKafkaDTO> implements TopicConsumer<String, KvpAvsluttetKafkaDTO> {
    private final KVPAvsluttetService kvpService;
    private final KvpHendelseDedupService dedupService;

    public KvpAvsluttetKafkaConsumer(
            KVPAvsluttetService kvpService,
            KvpHendelseDedupService dedupService,
            @Value("${topic.inn.kvpAvsluttet}")
                    String topic
    ) {
        super();
        this.kvpService = kvpService;
        this.dedupService = dedupService;

        this.setTopic(topic);
        this.setKeyDeserializer(Deserializers.stringDeserializer());
        this.setValueDeserializer(Deserializers.jsonDeserializer(KvpAvsluttetKafkaDTO.class));
        this.setConsumer(this);
    }

    @Override
    @Timed(value="kvp_avsluttet_consumer")
    public ConsumeStatus consume(ConsumerRecord<String, KvpAvsluttetKafkaDTO> consumerRecord) {
        KvpAvsluttetKafkaDTO kvpAvsluttetDto = consumerRecord.value();

        Person.AktorId aktorId = Person.aktorId(kvpAvsluttetDto.getAktorId());
        String begrunnelse = kvpAvsluttetDto.getAvsluttetBegrunnelse();
        Date sluttDato = new Date(kvpAvsluttetDto.getAvsluttetDato().toInstant().toEpochMilli());

        if (!dedupService.reserverAvsluttetHendelse(aktorId, sluttDato)) {
            return ConsumeStatus.OK;
        }

        kvpService.settAktiviteterInomKVPPeriodeTilAvbrutt(aktorId, begrunnelse, sluttDato);

        return ConsumeStatus.OK;
    }
}
