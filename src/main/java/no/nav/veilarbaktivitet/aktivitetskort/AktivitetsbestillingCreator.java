package no.nav.veilarbaktivitet.aktivitetskort;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import no.nav.common.json.JsonMapper;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.AktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.ArenaAktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.EksternAktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.dto.BestillingBase;
import no.nav.veilarbaktivitet.aktivitetskort.dto.KafkaAktivitetskortWrapperDTO;
import no.nav.veilarbaktivitet.aktivitetskort.feil.*;
import no.nav.veilarbaktivitet.arena.model.ArenaId;
import no.nav.veilarbaktivitet.person.IkkeFunnetPersonException;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.PersonService;
import no.nav.veilarbaktivitet.person.UgyldigIdentException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
        } catch (UgyldigIdentException e) {
            throw new UgyldigIdentFeil("Ugyldig ident for fnr :" + fnr.get(), e);
        } catch (IkkeFunnetPersonException e) {
            throw new UgyldigIdentFeil("AktørId ikke funnet for fnr :" + fnr.get(), e);
        }
    }
    public BestillingBase lagBestilling(ConsumerRecord<String, String> consumerRecord) throws DeserialiseringsFeil, UgyldigIdentFeil, IkkeFunnetPersonException, KeyErIkkeFunksjonellIdFeil, MessageIdIkkeUnikFeil {
        var melding = deserialiser(consumerRecord);
        if (!melding.getAktivitetskortId().toString().equals(consumerRecord.key()))
            throw new KeyErIkkeFunksjonellIdFeil(new ErrorMessage(String.format("aktivitetsId: %s må være lik kafka-meldings-id: %s", melding.getAktivitetskortId(), consumerRecord.key())), null);
        if (melding.getMessageId().equals(melding.getAktivitetskortId())) {
            throw new MessageIdIkkeUnikFeil(new ErrorMessage("messageId må være unik for hver melding. aktivitetsId er lik messageId"), null);
        }
        if (melding instanceof KafkaAktivitetskortWrapperDTO aktivitetskortMelding) {
            var aktorId = hentAktorId(Person.fnr(aktivitetskortMelding.getAktivitetskort().getPersonIdent()));
            boolean erArenaAktivitet = ARENA_TILTAK_AKTIVITET_ACL.equals(melding.getSource());
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
        byte[] eksternReferanseIdBytes = consumerRecord.headers().lastHeader(HEADER_EKSTERN_REFERANSE_ID).value();
        return new ArenaId(new String(eksternReferanseIdBytes));
    }

    private static String getArenaTiltakskode(ConsumerRecord<String, String> consumerRecord) {
        byte[] arenaTiltakskode = consumerRecord.headers().lastHeader(HEADER_EKSTERN_ARENA_TILTAKSKODE).value();
        return new String(arenaTiltakskode);
    }
}
