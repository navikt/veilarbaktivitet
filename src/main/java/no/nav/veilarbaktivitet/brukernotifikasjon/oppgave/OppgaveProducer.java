package no.nav.veilarbaktivitet.brukernotifikasjon.oppgave;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.nav.brukernotifikasjon.schemas.builders.NokkelInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.OppgaveInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.domain.PreferertKanal;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaAvroAvroTemplate;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@RequiredArgsConstructor
@Service
public class OppgaveProducer {
    private final KafkaAvroAvroTemplate<NokkelInput, OppgaveInput> kafkaOppgaveProducer;
    @Value("${topic.ut.brukernotifikasjon.oppgave}")
    private String oppgaveToppic;

    @Value("${app.env.appname}")
    private String appname;
    @Value("${app.env.namespace}")
    private String namespace;

    @SneakyThrows
    long sendOppgave(SkalSendes skalSendes) {
        int sikkerhetsnivaa = 3;

        NokkelInput nokkel = new NokkelInputBuilder()
                .withAppnavn(appname)
                .withNamespace(namespace)
                .withFodselsnummer(skalSendes.getFnr().get())
                .withGrupperingsId(skalSendes.getOppfolgingsperiode())
                .withEventId(skalSendes.getBrukernotifikasjonId())
                .build();

        OppgaveInput oppgave = new OppgaveInputBuilder()
                .withTidspunkt(LocalDateTime.now(ZoneOffset.UTC))
                .withTekst(skalSendes.getMelding())
                .withLink(skalSendes.getUrl())
                .withSikkerhetsnivaa(sikkerhetsnivaa)
                .withEksternVarsling(true)
                .withSmsVarslingstekst(skalSendes.getSmsTekst())
                .withSynligFremTil(LocalDateTime.now(ZoneOffset.UTC).plusMonths(1))
                .withPrefererteKanaler(PreferertKanal.SMS)
                .withSmsVarslingstekst(skalSendes.getSmsTekst()) //blir dafult tekst hvis null
                .withEpostVarslingstittel(skalSendes.getEpostTitel()) //blir dafult tekst hvis null
                .withEpostVarslingstekst(skalSendes.getEpostBody()) //blir dafult tekst hvis null
                .build();

        final ProducerRecord<NokkelInput, OppgaveInput> kafkaMelding = new ProducerRecord<>(oppgaveToppic, nokkel, oppgave);
        return kafkaOppgaveProducer.send(kafkaMelding).get().getRecordMetadata().offset();
    }

}
