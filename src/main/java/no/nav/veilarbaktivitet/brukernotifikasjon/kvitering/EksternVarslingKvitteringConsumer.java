package no.nav.veilarbaktivitet.brukernotifikasjon.kvitering;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


//TODO se på om schema bør vere dependency
@Service
@Slf4j
@RequiredArgsConstructor
public class EksternVarslingKvitteringConsumer {
    private final KvitteringDAO kvitteringDAO;
    private final KvitteringMetrikk kvitteringMetrikk;

    public static final String FEILET = "FEILET";
    public static final String INFO = "INFO";
    public static final String OVERSENDT = "OVERSENDT";
    public static final String FERDIGSTILT = "FERDIGSTILT";
    private static final String APP_NAME_TO_BRUKERNOTIFIKASJONER = "veilarbaktivitet";
    private static final String OPPGAVE_PREFIX ="O-" + APP_NAME_TO_BRUKERNOTIFIKASJONER + "-";
    private static final String BESKJED_PREFIX = "B-" + APP_NAME_TO_BRUKERNOTIFIKASJONER + "-";


    @Transactional
    @KafkaListener(topics = "${topic.inn.eksternVarselKvittering}", containerFactory = "stringAvroKafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, DoknotifikasjonStatus> kafkaRecord) {
        DoknotifikasjonStatus melding = kafkaRecord.value();
        String bestillerid =melding.getBestillerId();
        if (!APP_NAME_TO_BRUKERNOTIFIKASJONER.equals(melding.getBestillerId())) {
            return;
        }

        String brukernotifikasjonBestillingsId = melding.getBestillingsId();
        log.info("Konsumerer DoknotifikasjonStatus bestillingsId={}, status={}", brukernotifikasjonBestillingsId, melding.getStatus());

        if (!brukernotifikasjonBestillingsId.startsWith(OPPGAVE_PREFIX) && !brukernotifikasjonBestillingsId.startsWith(BESKJED_PREFIX)) {
            log.error("mottok melding med feil prefiks, {}", melding);
            throw new IllegalArgumentException("mottok melding med feil prefiks");
        }
        String bestillingsId = brukernotifikasjonBestillingsId.substring(OPPGAVE_PREFIX.length()); // Fjerner O eller B + - + srv + - som legges til av brukernotifikajson

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
