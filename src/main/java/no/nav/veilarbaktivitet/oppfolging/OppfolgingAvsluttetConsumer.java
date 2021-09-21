package no.nav.veilarbaktivitet.oppfolging;

import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.util.TopicConsumerConfig;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.service.AktivitetService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class OppfolgingAvsluttetConsumer extends TopicConsumerConfig<String, OppfolgingAvsluttetKafkaDTO> implements TopicConsumer<String, OppfolgingAvsluttetKafkaDTO> {
    private final AktivitetService service;

    public OppfolgingAvsluttetConsumer(
            AktivitetService service,
            @Value("${app.kafka.oppfolgingAvsluttetTopic}")
                    String toppic
    ) {
        super();
        this.service = service;

        this.setTopic(toppic);
        this.setKeyDeserializer(Deserializers.stringDeserializer());
        this.setValueDeserializer(Deserializers.jsonDeserializer(OppfolgingAvsluttetKafkaDTO.class));
        this.setConsumer(this);
    }

    @Override
    public ConsumeStatus consume(ConsumerRecord<String, OppfolgingAvsluttetKafkaDTO> record) {

        OppfolgingAvsluttetKafkaDTO oppfolgingAvsluttetDto = record.value();

        Person.AktorId aktorId = Person.aktorId(oppfolgingAvsluttetDto.getAktorId());
        Date sluttDato = new Date(oppfolgingAvsluttetDto.getSluttdato().toInstant().toEpochMilli());

        service.settAktiviteterTilHistoriske(aktorId, sluttDato);
        return ConsumeStatus.OK;
    }
}
