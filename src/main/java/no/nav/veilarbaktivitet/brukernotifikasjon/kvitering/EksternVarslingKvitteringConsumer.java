package no.nav.veilarbaktivitet.brukernotifikasjon.kvitering;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


//TODO se på om schema bør vere dependency
@Service
@Slf4j
public class EksternVarslingKvitteringConsumer {
    private final KvitteringDAO kvitteringDAO;
    private final KvitteringMetrikk kvitteringMetrikk;

    public static final String FEILET = "FEILET";
    public static final String INFO = "INFO";
    public static final String OVERSENDT = "OVERSENDT";
    public static final String FERDIGSTILT = "FERDIGSTILT";
    private final String oppgave_prefix;
    private final String beskjed_prefix;
    private final String appname;

    public EksternVarslingKvitteringConsumer(KvitteringDAO kvitteringDAO, KvitteringMetrikk kvitteringMetrikk, @Value("${app.env.appname}") String appname) {
        this.kvitteringDAO = kvitteringDAO;
        this.kvitteringMetrikk = kvitteringMetrikk;
        oppgave_prefix = "O-" + appname + "-";
        beskjed_prefix = "B-" + appname + "-";
        this.appname = appname;
    }


    @Transactional
    @KafkaListener(topics = "${topic.inn.eksternVarselKvittering}", containerFactory = "stringAvroKafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, DoknotifikasjonStatus> kafkaRecord) {
        DoknotifikasjonStatus melding = kafkaRecord.value();
        if (!appname.equals(melding.getBestillerId())) {
            return;
        }

        String brukernotifikasjonBestillingsId = melding.getBestillingsId();
        log.info("Konsumerer DoknotifikasjonStatus bestillingsId={}, status={}", brukernotifikasjonBestillingsId, melding.getStatus());

        if (!brukernotifikasjonBestillingsId.startsWith(oppgave_prefix) && !brukernotifikasjonBestillingsId.startsWith(beskjed_prefix)) {
            log.error("mottok melding med feil prefiks, {}", melding);
            throw new IllegalArgumentException("mottok melding med feil prefiks");
        }
        String bestillingsId = brukernotifikasjonBestillingsId.substring(oppgave_prefix.length()); // Fjerner O eller B + - + srv + - som legges til av brukernotifikajson

        String status = melding.getStatus();

        switch (status) {
            case INFO:
            case OVERSENDT:
                break;
            case FEILET:
                log.error("varsel feilet for notifikasjon bestillingsId={} med melding {}", brukernotifikasjonBestillingsId, melding.getMelding());
                kvitteringDAO.setFeilet(bestillingsId);
                break;
            case FERDIGSTILT:
                if (melding.getDistribusjonId() != null) {
                    // Kan komme første gang og på resendinger
                    kvitteringDAO.setFullfortForGyldige(bestillingsId);
                    log.info("Brukernotifikasjon fullført for bestillingsId={}", brukernotifikasjonBestillingsId);
                } else {
                    log.info("Hele bestillingen inkludert revarsling er ferdig, bestillingsId={}", brukernotifikasjonBestillingsId);
                }
                break;
            default:
                log.error("ukjent status for melding {}", melding);
                throw new IllegalArgumentException("ukjent status for melding");
        }

        if (melding.getDistribusjonId() == null) {
            kvitteringMetrikk.incrementBrukernotifikasjonKvitteringMottatt(status);
        }
    }
}
