package no.nav.veilarbaktivitet.brukernotifikasjon.avslutt;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.nav.brukernotifikasjon.schemas.builders.DoneInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.NokkelInputBuilder;
import no.nav.brukernotifikasjon.schemas.input.DoneInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaAvroAvroTemplate;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.PersonService;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
class AvsluttSender {
    private final KafkaAvroAvroTemplate<NokkelInput, DoneInput> producer;
    private final AvsluttDao avsluttDao;
    private final PersonService personService;

    @Value("${topic.ut.brukernotifikasjon.done}")
    private String doneToppic;

    @Value("${app.env.appname}")
    private String appname;
    @Value("${app.env.namespace}")
    private String namespace;


    @SneakyThrows
    @Transactional
    @Timed(value="brukernotifikasjon_avslutt_oppgave_sendt")
    public void avsluttOppgave(SkalAvluttes skalAvluttes) {
        String aktorId = skalAvluttes.getAktorId();
        String brukernotifikasjonId = skalAvluttes.getBrukernotifikasjonId();

        Person.Fnr fnrForAktorId = personService.getFnrForAktorId(Person.aktorId(aktorId));
        boolean markertAvsluttet = avsluttDao.markerOppgaveSomAvsluttet(brukernotifikasjonId);
        if (markertAvsluttet) {
            DoneInput done = new DoneInputBuilder()
                    .withTidspunkt(LocalDateTime.now(ZoneOffset.UTC))
                    .build();

            NokkelInput nokkel = new NokkelInputBuilder()
                    .withAppnavn(appname)
                    .withNamespace(namespace)
                    .withFodselsnummer(fnrForAktorId.get())
                    .withGrupperingsId(skalAvluttes.getOppfolgingsperiode().toString())
                    .withEventId(brukernotifikasjonId)
                    .build();
            final ProducerRecord<NokkelInput, DoneInput> kafkaMelding = new ProducerRecord<>(doneToppic, nokkel, done);

            producer.send(kafkaMelding).get();
        }
    }

    public List<SkalAvluttes> getOppgaverSomSkalAvbrytes(int maxAntall) {
        return avsluttDao.getOppgaverSomSkalAvsluttes(maxAntall);
    }

    public int avsluttIkkeSendteOppgaver() {
        return avsluttDao.avsluttIkkeSendteOppgaver();
    }

    public int markerAvslutteterAktiviteterSomSkalAvsluttes() {
        return avsluttDao.markerAvslutteterAktiviteterSomSkalAvsluttes();
    }
}
