package no.nav.veilarbaktivitet.brukernotifikasjon.oppgave;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.nav.brukernotifikasjon.schemas.Beskjed;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.builders.domain.PreferertKanal;
import no.nav.brukernotifikasjon.schemas.builders.legacy.BeskjedBuilder;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.utils.Credentials;
import no.nav.veilarbaktivitet.person.Person;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class BeskjedProducer {
    private final KafkaProducerClient<Nokkel, Beskjed> kafkaBeskjedProducer;
    private final Credentials serviceUserCredentials;
    @Value("${topic.ut.brukernotifikasjon.beskjed}")
    private String beskjedToppic;


    @SneakyThrows
    public long sendBeskjed(SkalSendes skalSendes, Person.Fnr fnr, URL aktivitetLink) {
        int sikkerhetsnivaa = 3;
        Nokkel nokkel = new Nokkel(serviceUserCredentials.username, skalSendes.getBrukernotifikasjonId());

        Beskjed beskjed = new BeskjedBuilder()
                .withTidspunkt(LocalDateTime.now(ZoneOffset.UTC))
                .withFodselsnummer(fnr.get())
                .withGrupperingsId(skalSendes.getOppfolgingsperiode())
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

        final ProducerRecord<Nokkel, Beskjed> kafkaMelding = new ProducerRecord<>(beskjedToppic, nokkel, beskjed);
        return kafkaBeskjedProducer.send(kafkaMelding).get().offset();
    }
}
