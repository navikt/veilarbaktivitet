package no.nav.veilarbaktivitet.brukernotifikasjon.kvitering;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.util.TopicConsumerConfig;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


//TODO se på om schema bør vere dependency
@Service
@Slf4j
public class Consumer extends TopicConsumerConfig<String, DoknotifikasjonStatus> implements TopicConsumer<String, DoknotifikasjonStatus> {
    private final KviteringDto kviteringDto;
    private static final String FEILET = "FEILET";
    private static final String INFO = "INFO";
    private static final String OVERSENDT = "OVERSENDT";
    private static final String FERDISTSTILT = "FERDISTSTILT";
    String srv = "srvveilarbaktivitet"; //TODO hent inn srv som bønne

    public Consumer(
            KviteringDto kviteringDto,
            Deserializer<DoknotifikasjonStatus> deserializer,
            @Value("${app.kafka.kvpAvsluttetTopic}")
                    String toppic

    ) {
        super();
        this.kviteringDto = kviteringDto;

        this.setTopic(toppic);
        this.setKeyDeserializer(Deserializers.stringDeserializer());
        this.setValueDeserializer(deserializer);
        this.setConsumer(this);
    }

    @Override
    public ConsumeStatus consume(ConsumerRecord<String, DoknotifikasjonStatus> kafkaRecord) {
        DoknotifikasjonStatus melding = kafkaRecord.value();
        if (!srv.equals(melding.getBestillerId())) {
            return ConsumeStatus.OK;
        }
        String brukernotifikasjonBestillingsId = melding.getBestillingsId();
        String bestillingsId = brukernotifikasjonBestillingsId.substring(3 + srv.length());//fjerner O eller B + - + srv + - som legges til av brukernotifikajson
        String status = melding.getStatus();

        switch (status) {
            case INFO:
            case OVERSENDT:
                log.info("motokk melding {}", melding);
                break;
            case FEILET:
                log.error("varsel feilet for melding {}", melding);
                kviteringDto.setFeilet(bestillingsId); //TODO finn ut hva vi bør gjøre med feilete varsler.
                break;
            case FERDISTSTILT:
                kviteringDto.setFulfortForGyldige(bestillingsId);
                break;
            default:
                log.error("uskjent status for melding {}", melding);
                return ConsumeStatus.FAILED;
        }

        return ConsumeStatus.OK;
    }
}
