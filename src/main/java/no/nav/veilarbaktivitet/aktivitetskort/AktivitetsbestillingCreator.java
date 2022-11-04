package no.nav.veilarbaktivitet.aktivitetskort;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import no.nav.common.json.JsonMapper;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.AktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.ArenaAktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.EksternAktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.feil.DeserialiseringsFeil;
import no.nav.veilarbaktivitet.aktivitetskort.feil.ErrorMessage;
import no.nav.veilarbaktivitet.aktivitetskort.feil.UgyldigIdentFeil;
import no.nav.veilarbaktivitet.arena.model.ArenaId;
import no.nav.veilarbaktivitet.person.IkkeFunnetPersonException;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.PersonService;
import no.nav.veilarbaktivitet.person.UgyldigIdentException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

import static no.nav.veilarbaktivitet.aktivitetskort.MeldingContext.HEADER_EKSTERN_ARENA_TILTAKSKODE;
import static no.nav.veilarbaktivitet.aktivitetskort.MeldingContext.HEADER_EKSTERN_REFERANSE_ID;

@Component
@RequiredArgsConstructor
public class AktivitetsbestillingCreator {
    private final PersonService personService;
    public static final String ARENA_TILTAK_AKTIVITET_ACL = "ARENA_TILTAK_AKTIVITET_ACL";
    private static ObjectMapper objectMapper = null; //JsonMapper.defaultObjectMapper();
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
    private Person.AktorId hentAktorId(Person.Fnr fnr) throws UgyldigIdentFeil {
        try {
            return personService.getAktorIdForPersonBruker(fnr).orElseThrow(() -> new UgyldigIdentFeil("Ugyldig identtype for " + fnr.get(), null));
        } catch (UgyldigIdentException e) {
            throw new UgyldigIdentFeil("Ugyldig ident for fnr :" + fnr.get(), e);
        } catch (IkkeFunnetPersonException e) {
            throw new UgyldigIdentFeil("AktørId ikke funnet for fnr :" + fnr.get(), e);
        }
    }
    public AktivitetskortBestilling lagBestilling(ConsumerRecord<String, String> consumerRecord) throws DeserialiseringsFeil, UgyldigIdentFeil, IkkeFunnetPersonException {
        var melding = deserialiser(consumerRecord);
        var aktorId = hentAktorId(Person.fnr(melding.aktivitetskort.getPersonIdent()));
        boolean erArenaAktivitet = ARENA_TILTAK_AKTIVITET_ACL.equals(melding.source);
        if (erArenaAktivitet) {
            return new ArenaAktivitetskortBestilling(
                melding.aktivitetskort,
                melding.source,
                melding.aktivitetskortType,
                getEksternReferanseId(consumerRecord),
                getArenaTiltakskode(consumerRecord),
                melding.messageId,
                melding.actionType,
                aktorId
            );
        } else {
            return new EksternAktivitetskortBestilling(
                melding.aktivitetskort,
                melding.source,
                melding.aktivitetskortType,
                melding.messageId,
                melding.actionType,
                aktorId
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
