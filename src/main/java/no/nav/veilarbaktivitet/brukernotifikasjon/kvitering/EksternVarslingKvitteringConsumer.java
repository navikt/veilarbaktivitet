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
    public static final String FEILET = "FEILET";
    public static final String INFO = "INFO";
    public static final String OVERSENDT = "OVERSENDT";
    public static final String FERDIGSTILT = "FERDIGSTILT";
    private final String srvUsername;
    private final String oppgavePrefix;
    private final String beskjedPrefix;
    private final KvitteringMetrikk kvitteringMetrikk;

    public EksternVarslingKvitteringConsumer(
            KvitteringDAO kvitteringDAO,
            Credentials credentials,
            Deserializer<DoknotifikasjonStatus> deserializer,
            @Value("${topic.inn.eksternVarselKvittering}")
                    String topic,
            KvitteringMetrikk kvitteringMetrikk
    ) {
        super();
        this.kvitteringDAO = kvitteringDAO;

        srvUsername = credentials.username;
        oppgavePrefix = "O-" + srvUsername + "-";
        beskjedPrefix = "B-" + srvUsername + "-";
        this.setTopic(topic);
        this.setKeyDeserializer(Deserializers.stringDeserializer());
        this.setValueDeserializer(deserializer);
        this.setConsumer(this);
        this.kvitteringMetrikk = kvitteringMetrikk;
    }

    @Override
    public ConsumeStatus consume(ConsumerRecord<String, DoknotifikasjonStatus> kafkaRecord) {
        DoknotifikasjonStatus melding = kafkaRecord.value();
        if (!srvUsername.equals(melding.getBestillerId())) {
            return ConsumeStatus.OK;
        }

        String brukernotifikasjonBestillingsId = melding.getBestillingsId();
        log.info("Konsumerer DoknotifikasjonStatus bestillingsId={}, status={}", brukernotifikasjonBestillingsId, melding.getStatus());

        if (!brukernotifikasjonBestillingsId.startsWith(oppgavePrefix) && !brukernotifikasjonBestillingsId.startsWith(beskjedPrefix)) {
            log.warn("mottok melding med feil prefiks, {}", melding); //TODO finn ut om vi produserer på samme topic?
            return ConsumeStatus.FAILED;
        }
        String bestillingsId = brukernotifikasjonBestillingsId.substring(oppgavePrefix.length());//fjerner O eller B + - + srv + - som legges til av brukernotifikajson

        String status = melding.getStatus();

        switch (status) {
            case INFO:
            case OVERSENDT:
                break;
            case FEILET:
                log.error("varsel feilet for notifikasjon bestillingsId={} med melding {}", bestillingsId, melding.getMelding());
                kvitteringDAO.setFeilet(bestillingsId);
                break;
            case FERDIGSTILT:
                if (melding.getDistribusjonId() != null) {
                    // Første-gangs distribusjon ok
                    kvitteringDAO.setFullfortForGyldige(bestillingsId);
                    try {
                        kvitteringMetrikk.registrerTidBrukt(KvitteringMetrikk.IntervalNavn.FORSOKT_SENDT_BEKREFTET_SENDT_DIFF, kvitteringDAO.hentTidBrukt(bestillingsId));
                    } catch (Exception e) {
                        log.error("metrikk feilet", e);
                    }
                } else {
                    // revarling ok
                    kvitteringDAO.setRevarslet(bestillingsId);
                    log.info("Revarsling ok for bestillingsId={}", brukernotifikasjonBestillingsId);
                }
                break;
            default:
                log.error("ukjent status for melding {}", melding);
                return ConsumeStatus.FAILED;
        }

        if (melding.getDistribusjonId() == null) {
            kvitteringMetrikk.incrementBrukernotifikasjonKvitteringMottatt(status);
        }

        return ConsumeStatus.OK;
    }
}
