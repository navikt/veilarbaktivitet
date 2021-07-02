package no.nav.veilarbaktivitet.config.kafka;

import lombok.RequiredArgsConstructor;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.veilarbaktivitet.avro.ForesporselOmDelingAvCv;
import no.nav.veilarbaktivitet.deling_av_cv.OpprettForesporselOmDelingAvCv;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AvroTopicConsumer implements TopicConsumer<String, ForesporselOmDelingAvCv> {

    private final OpprettForesporselOmDelingAvCv opprettForesporselOmDelingAvCv;
    private final String topic = "deling-av-stilling-fra-nav-forespurt-v1"; //TODO: fiks denne

    @Override
    public ConsumeStatus consume(ConsumerRecord<String, ForesporselOmDelingAvCv> consumerRecord) {
        opprettForesporselOmDelingAvCv.createAktivitet(consumerRecord.value());
        return ConsumeStatus.OK;
    }

    public String getTopic() {
        return topic;
    }
}
