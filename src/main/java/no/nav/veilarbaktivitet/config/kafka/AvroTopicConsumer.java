package no.nav.veilarbaktivitet.config.kafka;

import lombok.RequiredArgsConstructor;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.veilarbaktivitet.avro.ForesporselOmDelingAvCv;
import no.nav.veilarbaktivitet.deling_av_cv.OpprettForesporselOmDelingAvCv;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class AvroTopicConsumer<K, V extends GenericRecord> implements TopicConsumer<K, V> {

    private final Function<V, Void> consumerFunction;
    private final String topic = "deling-av-stilling-fra-nav-forespurt-v1"; //TODO: fiks denne

    public String getTopic() {
        return topic;
    }

    @Override
    public ConsumeStatus consume(ConsumerRecord<K, V> record) {
        consumerFunction.apply(record.value());
        return ConsumeStatus.OK;
    }
}
