package no.nav.veilarbaktivitet.aktivitetskort;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import no.nav.common.json.JsonMapper;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.AktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.ArenaAktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.EksternAktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.service.AktivitetsBestillingDelegate;
import no.nav.veilarbaktivitet.arena.model.ArenaId;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

import static no.nav.veilarbaktivitet.aktivitetskort.MeldingContext.HEADER_EKSTERN_ARENA_TILTAKSKODE;
import static no.nav.veilarbaktivitet.aktivitetskort.MeldingContext.HEADER_EKSTERN_REFERANSE_ID;

@Component
@RequiredArgsConstructor
class AktivitetsbestillingCreator {
    public static final String ARENA_TILTAK_AKTIVITET_ACL = "ARENA_TILTAK_AKTIVITET_ACL";
    private static ObjectMapper objectMapper = JsonMapper.defaultObjectMapper();
    private static ObjectMapper getMapper() {
        if (objectMapper != null) return objectMapper;
        objectMapper = JsonMapper.defaultObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        return objectMapper;
    }
    private static KafkaAktivitetskortWrapperDTO deserialiser(ConsumerRecord<String, String> consumerRecord) throws DeserialiseringsFeil {
        try {
            return getMapper().readValue(consumerRecord.value(), KafkaAktivitetskortWrapperDTO.class);
        } catch (Exception e) {
            throw new DeserialiseringsFeil(
                    new ErrorMessage(e.getMessage()),
                    e
            );
        }
    }
    public AktivitetskortBestilling lagBestilling(ConsumerRecord<String, String> consumerRecord) throws DeserialiseringsFeil {
        var melding = deserialiser(consumerRecord);
        boolean erArenaAktivitet = ARENA_TILTAK_AKTIVITET_ACL.equals(melding.source);
        if (erArenaAktivitet) {
            return new ArenaAktivitetskortBestilling(
                melding.aktivitetskort,
                melding.source,
                melding.aktivitetskortType,
                getEksternReferanseId(consumerRecord),
                getArenaTiltakskode(consumerRecord),
                melding.messageId,
                melding.actionType
            );
        } else {
            return new EksternAktivitetskortBestilling(
                melding.aktivitetskort,
                melding.source,
                melding.aktivitetskortType,
                melding.messageId,
                melding.actionType
            );
        }
    }

    private static ArenaId getEksternReferanseId(ConsumerRecord<String, String> consumerRecord) {
        byte[] eksternReferanseIdBytes = consumerRecord.headers().lastHeader(HEADER_EKSTERN_REFERANSE_ID).value();
        return new ArenaId(new String(eksternReferanseIdBytes));
    }

    private static String getArenaTiltakskode(ConsumerRecord<String, String> consumerRecord) {
        byte[] arenaTiltakskode = consumerRecord.headers().lastHeader(HEADER_EKSTERN_ARENA_TILTAKSKODE).value();
        return new String(arenaTiltakskode);
    }
}
