package no.nav.veilarbaktivitet.brukernotifikasjon.oppgave;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.nav.brukernotifikasjon.schemas.builders.BeskjedInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.NokkelInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.domain.PreferertKanal;
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaAvroAvroTemplate;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class BeskjedProducer {
    private final KafkaAvroAvroTemplate<NokkelInput, BeskjedInput> kafkaBeskjedProducer;

    @Value("${topic.ut.brukernotifikasjon.beskjed}")
    private String beskjedToppic;

    @Value("${app.env.appname}")
    private String appname;
    @Value("${app.env.namespace}")
    private String namespace;

    @SneakyThrows
    public long sendBeskjed(SkalSendes skalSendes) {
        int sikkerhetsnivaa = 3;
        NokkelInput nokkel = new NokkelInputBuilder()
                .withAppnavn(appname)
                .withNamespace(namespace)
                .withFodselsnummer(skalSendes.getFnr().get())
                .withGrupperingsId(skalSendes.getOppfolgingsperiode())
                .withEventId(skalSendes.getBrukernotifikasjonId())
                .build();

        BeskjedInput beskjed = new BeskjedInputBuilder()
                .withTidspunkt(LocalDateTime.now(ZoneOffset.UTC))
                .withTekst(skalSendes.getMelding())
                .withLink(skalSendes.getUrl())
                .withSikkerhetsnivaa(sikkerhetsnivaa)
                .withEksternVarsling(true)
                .withSmsVarslingstekst(skalSendes.getSmsTekst())
                .withPrefererteKanaler(PreferertKanal.SMS)
                .withSmsVarslingstekst(skalSendes.getSmsTekst()) //blir dafult tekst hvis null
                .withEpostVarslingstittel(skalSendes.getEpostTitel()) //blir dafult tekst hvis null
                .withEpostVarslingstekst(skalSendes.getEpostBody())
                .build();

        final ProducerRecord<NokkelInput, BeskjedInput> kafkaMelding = new ProducerRecord<>(beskjedToppic, nokkel, beskjed);
        return kafkaBeskjedProducer.send(kafkaMelding).get().getRecordMetadata().offset();
    }
}
