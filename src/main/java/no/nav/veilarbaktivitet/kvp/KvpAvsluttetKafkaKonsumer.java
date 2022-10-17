package no.nav.veilarbaktivitet.kvp;

import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import no.nav.veilarbaktivitet.config.kafka.OnpremConsumerConfig;
import no.nav.veilarbaktivitet.person.Person;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;


@Service
public class KvpAvsluttetKafkaKonsumer extends OnpremConsumerConfig<String, KvpAvsluttetKafkaDTO> implements TopicConsumer<String, KvpAvsluttetKafkaDTO> {
    private final KVPAvsluttetService kvpService;

    public KvpAvsluttetKafkaKonsumer(
            KVPAvsluttetService kvpService,
            @Value("${app.kafka.kvpAvsluttetTopic}")
                    String toppic
    ) {
        super();
        this.kvpService = kvpService;

        this.setTopic(toppic);
        this.setKeyDeserializer(Deserializers.stringDeserializer());
        this.setValueDeserializer(Deserializers.jsonDeserializer(KvpAvsluttetKafkaDTO.class));
        this.setConsumer(this);
    }

    @Override
    public ConsumeStatus consume(ConsumerRecord<String, KvpAvsluttetKafkaDTO> record) {
        KvpAvsluttetKafkaDTO kvpAvsluttetDto = record.value();

        Person.AktorId aktorId = Person.aktorId(kvpAvsluttetDto.getAktorId());
        String begrunnelse = kvpAvsluttetDto.getAvsluttetBegrunnelse();
        Date sluttDato = new Date(kvpAvsluttetDto.getAvsluttetDato().toInstant().toEpochMilli());

        kvpService.settAktiviteterInomKVPPeriodeTilAvbrutt(aktorId, begrunnelse, sluttDato);

        return ConsumeStatus.OK;
    }
}
