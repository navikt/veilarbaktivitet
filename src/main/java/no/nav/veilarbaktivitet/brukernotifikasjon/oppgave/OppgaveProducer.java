package no.nav.veilarbaktivitet.brukernotifikasjon.oppgave;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.nav.brukernotifikasjon.schemas.builders.NokkelInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.OppgaveInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.domain.PreferertKanal;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import no.nav.common.utils.Credentials;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaAvroAvroTemplate;
import no.nav.veilarbaktivitet.person.Person;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@RequiredArgsConstructor
@Service
public class OppgaveProducer {
    private final KafkaAvroAvroTemplate<NokkelInput, OppgaveInput> kafkaOppgaveProducer;
    private final Credentials serviceUserCredentials;
    @Value("${topic.ut.brukernotifikasjon.oppgave}")
    private String oppgaveToppic;

    @Value("${app.env.appname}")
    private String appname;
    @Value("${app.env.namespace}")
    private String namespace;

    @SneakyThrows
    long sendOppgave(SkalSendes skalSendes, Person.Fnr fnr, URL aktivitetLink) {
        int sikkerhetsnivaa = 3;

        NokkelInput nokkel = new NokkelInputBuilder()
                .withAppnavn(appname)
                .withNamespace(namespace)
                .withFodselsnummer(fnr.get())
                .withGrupperingsId(skalSendes.getOppfolgingsperiode())
                .withEventId(skalSendes.getBrukernotifikasjonId())
                .build();

        OppgaveInput oppgave = new OppgaveInputBuilder()
                .withTidspunkt(LocalDateTime.now(ZoneOffset.UTC))
                .withTekst(skalSendes.getMelding())
                .withLink(aktivitetLink)
                .withSikkerhetsnivaa(sikkerhetsnivaa)
                .withEksternVarsling(true)
                .withSmsVarslingstekst(skalSendes.getSmsTekst())
                .withPrefererteKanaler(PreferertKanal.SMS)
                .withSmsVarslingstekst(skalSendes.getSmsTekst()) //blir dafult tekst hvis null
                .withEpostVarslingstittel(skalSendes.getEpostTitel()) //blir dafult tekst hvis null
                .withEpostVarslingstekst(skalSendes.getEpostBody()) //blir dafult tekst hvis null
                .build();

        final ProducerRecord<NokkelInput, OppgaveInput> kafkaMelding = new ProducerRecord<>(oppgaveToppic, nokkel, oppgave);
        return kafkaOppgaveProducer.send(kafkaMelding).get().getRecordMetadata().offset();
    }

}
