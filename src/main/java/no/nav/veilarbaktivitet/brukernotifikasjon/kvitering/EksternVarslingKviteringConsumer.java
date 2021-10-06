package no.nav.veilarbaktivitet.brukernotifikasjon.kvitering;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.util.TopicConsumerConfig;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import no.nav.common.utils.Credentials;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


//TODO se på om schema bør vere dependency
@Service
@Slf4j
public class EksternVarslingKviteringConsumer extends TopicConsumerConfig<String, DoknotifikasjonStatus> implements TopicConsumer<String, DoknotifikasjonStatus> {
    private final KviteringDto kviteringDto;
    private static final String FEILET = "FEILET";
    private static final String INFO = "INFO";
    private static final String OVERSENDT = "OVERSENDT";
    private static final String FERDISTSTILT = "FERDISTSTILT";
    private final String srvUsername;

    public EksternVarslingKviteringConsumer(
            KviteringDto kviteringDto,
            Credentials credentials,
            Deserializer<DoknotifikasjonStatus> deserializer,
            @Value("${topic.inn.ekstertVarselKvitering}")
                    String toppic
    ) {
        super();
        this.kviteringDto = kviteringDto;

        srvUsername = credentials.username;
        this.setTopic(toppic);
        this.setKeyDeserializer(Deserializers.stringDeserializer());
        this.setValueDeserializer(deserializer);
        this.setConsumer(this);
    }

    @Override
    public ConsumeStatus consume(ConsumerRecord<String, DoknotifikasjonStatus> kafkaRecord) {
        DoknotifikasjonStatus melding = kafkaRecord.value();
        if (!srvUsername.equals(melding.getBestillerId())) {
            return ConsumeStatus.OK;
        }
        String brukernotifikasjonBestillingsId = melding.getBestillingsId();
        String bestillingsId = brukernotifikasjonBestillingsId.substring(3 + srvUsername.length());//fjerner O eller B + - + srv + - som legges til av brukernotifikajson
        String status = melding.getStatus();

        switch (status) {
            case INFO:
            case OVERSENDT:
                log.info("motokk melding {}", melding);
                break;
            case FEILET:
                log.error("varsel feilet for melding {}", melding);
                kviteringDto.setFeilet(bestillingsId);
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
