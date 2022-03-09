package no.nav.veilarbaktivitet.brukernotifikasjon.oppgave;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.nav.brukernotifikasjon.schemas.builders.domain.PreferertKanal;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.utils.Credentials;
import no.nav.veilarbaktivitet.person.Person;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@RequiredArgsConstructor
@Service
public class OppgaveProducer {
    private final KafkaProducerClient<NokkelInput, OppgaveInput> kafkaOppgaveProducer;
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

        NokkelInput nokkel = NokkelInput.newBuilder()
                .setAppnavn(appname)
                .setNamespace(namespace)
                .setFodselsnummer(fnr.get())
                .setGrupperingsId(skalSendes.getOppfolgingsperiode())
                .setEventId(skalSendes.getBrukernotifikasjonId())
                .build();

        OppgaveInput oppgave = OppgaveInput.newBuilder()
                .setTidspunkt(LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC))
                .setTekst(skalSendes.getMelding())
                .setLink(aktivitetLink.toString())
                .setSikkerhetsnivaa(sikkerhetsnivaa)
                .setEksternVarsling(true)
                .setSmsVarslingstekst(skalSendes.getSmsTekst())
                .setPrefererteKanaler(List.of(PreferertKanal.SMS.toString()))
                .setSmsVarslingstekst(skalSendes.getSmsTekst()) //blir dafult tekst hvis null
                .setEpostVarslingstittel(skalSendes.getEpostTitel()) //blir dafult tekst hvis null
                .setEpostVarslingstekst(skalSendes.getEpostBody()) //blir dafult tekst hvis null
                .build();

        final ProducerRecord<NokkelInput, OppgaveInput> kafkaMelding = new ProducerRecord<>(oppgaveToppic, nokkel, oppgave);
        return kafkaOppgaveProducer.send(kafkaMelding).get().offset();
    }

}
