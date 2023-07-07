package no.nav.veilarbaktivitet.aktivitetskort;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import no.nav.common.client.aktorregister.IngenGjeldendeIdentException;
import no.nav.common.json.JsonMapper;
import no.nav.veilarbaktivitet.aktivitetskort.dto.bestilling.ArenaAktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.dto.bestilling.EksternAktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.dto.BestillingBase;
import no.nav.veilarbaktivitet.aktivitetskort.dto.KafkaAktivitetskortWrapperDTO;
import no.nav.veilarbaktivitet.aktivitetskort.feil.*;
import no.nav.veilarbaktivitet.arena.model.ArenaId;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.PersonService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class AktivitetsbestillingCreator {
    public static final String HEADER_EKSTERN_REFERANSE_ID = "eksternReferanseId";
    public static final String HEADER_EKSTERN_ARENA_TILTAKSKODE = "arenaTiltakskode";
    private final PersonService personService;
    public static final String ARENA_TILTAK_AKTIVITET_ACL = "ARENA_TILTAK_AKTIVITET_ACL";
    private static ObjectMapper objectMapper = null;
    private static ObjectMapper getMapper() {
        if (objectMapper != null) return objectMapper;
        objectMapper = JsonMapper.defaultObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        return objectMapper;
    }
    static BestillingBase deserialiser(ConsumerRecord<String, String> consumerRecord) throws DeserialiseringsFeil {
        try {
            return getMapper().readValue(consumerRecord.value(), BestillingBase.class);
        } catch (Exception e) {
            throw new DeserialiseringsFeil(
                    new ErrorMessage(e.getMessage()),
                    e
            );
        }
    }
    private Person.AktorId hentAktorId(Person.Fnr fnr) throws UgyldigIdentFeil {
        try {
            return personService.getAktorIdForPersonBruker(fnr).orElseThrow(() -> new UgyldigIdentFeil("Ugyldig identtype for " + fnr.get(), null));
        } catch (IngenGjeldendeIdentException e) {
            throw new UgyldigIdentFeil("Ident ikke funnet eller ugyldig ident for fnr :" + fnr.get(), e);
        }
    }

    @Timed(value="akaas_lagBestilling")
    public BestillingBase lagBestilling(ConsumerRecord<String, String> consumerRecord) throws DeserialiseringsFeil, UgyldigIdentFeil, KeyErIkkeFunksjonellIdFeil {
        var melding = deserialiser(consumerRecord);
        if (!melding.getAktivitetskortId().toString().equals(consumerRecord.key()))
            throw new KeyErIkkeFunksjonellIdFeil(new ErrorMessage(String.format("aktivitetsId: %s må være lik kafka-meldings-id: %s", melding.getAktivitetskortId(), consumerRecord.key())), null);
        if (melding instanceof KafkaAktivitetskortWrapperDTO aktivitetskortMelding) {
            var aktorId = hentAktorId(Person.fnr(aktivitetskortMelding.getAktivitetskort().getPersonIdent()));
            boolean erArenaAktivitet = AktivitetskortType.ARENA_TILTAK.equals(aktivitetskortMelding.getAktivitetskortType());
            if (erArenaAktivitet) {
                return new ArenaAktivitetskortBestilling(
                        aktivitetskortMelding.getAktivitetskort(),
                        aktivitetskortMelding.getSource(),
                        aktivitetskortMelding.getAktivitetskortType(),
                        getEksternReferanseId(consumerRecord),
                        getArenaTiltakskode(consumerRecord),
                        aktivitetskortMelding.getMessageId(),
                        aktivitetskortMelding.getActionType(),
                        aktorId
                );
            } else {
                return new EksternAktivitetskortBestilling(
                        aktivitetskortMelding.getAktivitetskort(),
                        aktivitetskortMelding.getSource(),
                        aktivitetskortMelding.getAktivitetskortType(),
                        aktivitetskortMelding.getMessageId(),
                        aktivitetskortMelding.getActionType(),
                        aktorId
                );
            }
        } else {
            return melding;
        }
    }

    private static ArenaId getEksternReferanseId(ConsumerRecord<String, String> consumerRecord) {
        Header header = consumerRecord.headers().lastHeader(HEADER_EKSTERN_REFERANSE_ID);
        if (header == null) throw new RuntimeException("Mangler Arena Header for ArenaTiltak aktivitetskort");
        byte[] eksternReferanseIdBytes = header.value();
        return new ArenaId(new String(eksternReferanseIdBytes));
    }

    private static String getArenaTiltakskode(ConsumerRecord<String, String> consumerRecord) {
        Header header = consumerRecord.headers().lastHeader(HEADER_EKSTERN_ARENA_TILTAKSKODE);
        if (header == null) throw new RuntimeException("Mangler Arena Header for ArenaTiltak aktivitetskort");
        byte[] arenaTiltakskode = header.value();
        return new String(arenaTiltakskode);
    }
}
