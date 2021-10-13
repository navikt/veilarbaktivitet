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
public class EksternVarslingKvitteringConsumer extends TopicConsumerConfig<String, DoknotifikasjonStatus> implements TopicConsumer<String, DoknotifikasjonStatus> {
    private final KvitteringDAO kvitteringDAO;
    private static final String FEILET = "FEILET";
    private static final String INFO = "INFO";
    private static final String OVERSENDT = "OVERSENDT";
    private static final String FERDISTSTILT = "FERDISTSTILT";
    private final String srvUsername;
    private final String oppgavePrefix;
    private final String beskjedPrefix;

    public EksternVarslingKvitteringConsumer(
            KvitteringDAO kvitteringDAO,
            Credentials credentials,
            Deserializer<DoknotifikasjonStatus> deserializer,
            @Value("${topic.inn.ekstertVarselKvitering}")
                    String toppic
    ) {
        super();
        this.kvitteringDAO = kvitteringDAO;

        srvUsername = credentials.username;
        oppgavePrefix = "O-" + srvUsername + "-";
        beskjedPrefix = "B-" + srvUsername + "-";
        this.setTopic(toppic);
        this.setKeyDeserializer(Deserializers.stringDeserializer());
        this.setValueDeserializer(deserializer);
        this.setConsumer(this);
    }

    @Override
    public ConsumeStatus consume(ConsumerRecord<String, DoknotifikasjonStatus> kafkaRecord) {
        DoknotifikasjonStatus melding = kafkaRecord.value();
        if (!srvUsername.equals(melding.getBestillerId())) {
            log.info("avsemder {}", melding.getBestillerId());
            log.info(" bestillingsid {}", melding.getBestillingsId());
            return ConsumeStatus.OK;
        }

        String brukernotifikasjonBestillingsId = melding.getBestillingsId();

        if (!brukernotifikasjonBestillingsId.startsWith(oppgavePrefix) && !brukernotifikasjonBestillingsId.startsWith(beskjedPrefix)) {
            log.warn("mottok melding med feil prefiks, {}", melding); //TODO finn ut om vi produserer på samme topic?
            return ConsumeStatus.FAILED;
        }
        String bestillingsId = brukernotifikasjonBestillingsId.substring(oppgavePrefix.length());//fjerner O eller B + - + srv + - som legges til av brukernotifikajson

        String status = melding.getStatus();
        log.info("mottokk melding {}", melding);

        switch (status) {
            case INFO:
            case OVERSENDT:
                break;
            case FEILET:
                log.error("varsel feilet for melding {}", melding);
                kvitteringDAO.setFeilet(bestillingsId);
                break;
            case FERDISTSTILT:
                kvitteringDAO.setFullfortForGyldige(bestillingsId);
                break;
            default:
                log.error("ukjent status for melding {}", melding);
                return ConsumeStatus.FAILED;
        }

        return ConsumeStatus.OK;
    }
}
