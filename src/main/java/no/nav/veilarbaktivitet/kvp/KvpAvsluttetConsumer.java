package no.nav.veilarbaktivitet.kvp;

import io.micrometer.core.annotation.Timed;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.veilarbaktivitet.person.Person;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

import java.util.Date;


@Service
@RequiredArgsConstructor
@EqualsAndHashCode
public class KvpAvsluttetConsumer implements TopicConsumer<String, KvpAvsluttetDTO> {

    private final KVPAvsluttetService kvpService;

    @Override
    @Timed(value="kvp_avsluttet_consumer")
    public ConsumeStatus consume(ConsumerRecord<String, KvpAvsluttetDTO> consumerRecord) {
        KvpAvsluttetDTO kvpAvsluttetDto = consumerRecord.value();

        Person.AktorId aktorId = Person.aktorId(kvpAvsluttetDto.getAktorId());
        String begrunnelse = kvpAvsluttetDto.getAvsluttetBegrunnelse();
        Date sluttDato = new Date(kvpAvsluttetDto.getAvsluttetDato().toInstant().toEpochMilli());

        kvpService.settAktiviteterInomKVPPeriodeTilAvbrutt(aktorId, begrunnelse, sluttDato);

        return ConsumeStatus.OK;
    }
}
