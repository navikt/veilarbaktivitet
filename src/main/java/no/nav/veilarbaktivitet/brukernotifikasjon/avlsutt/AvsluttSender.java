package no.nav.veilarbaktivitet.brukernotifikasjon.avlsutt;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.nav.brukernotifikasjon.schemas.Done;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.utils.Credentials;
import no.nav.veilarbaktivitet.person.AuthService;
import no.nav.veilarbaktivitet.person.Person;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
class AvsluttSender {
    private final KafkaProducerClient<Nokkel, Done> producer;
    private final AvsluttDao avsluttDao;
    private final AuthService authService;
    private final Credentials serviceUserCredentials;

    @Value("${topic.ut.brukernotifikasjon.done}")
    private String doneToppic;


    @SneakyThrows
    @Transactional
    public void avsluttOppgave(SkalAvluttes skalAvluttes) {
        String aktorId = skalAvluttes.getAktorId();
        String brukernotifikasjonId = skalAvluttes.getBrukernotifikasjonId();

        Person.Fnr fnrForAktorId = authService.getFnrForAktorId(Person.aktorId(aktorId));
        boolean markertAvsluttet = avsluttDao.markerOppgaveSomAvbrutt(brukernotifikasjonId);
        if (markertAvsluttet) {
            Done done = Done
                    .newBuilder()
                    .setFodselsnummer(fnrForAktorId.get())
                    .setGrupperingsId(skalAvluttes.getOppfolgingsPeriode().toString())
                    .setTidspunkt(Instant.now().toEpochMilli())
                    .build();

            Nokkel nokkel = new Nokkel(serviceUserCredentials.username, brukernotifikasjonId);
            final ProducerRecord<Nokkel, Done> kafkaMelding = new ProducerRecord<>(doneToppic, nokkel, done);

            producer.send(kafkaMelding).get();
        }
    }

    public List<SkalAvluttes> getOppgaverSomSkalAvbrytes(int maxAntall) {
        return avsluttDao.getOppgaverSomSkalAvsluttes(maxAntall);
    }

    public int avsluttIkkeSendteOppgaver() {
        return avsluttDao.avsluttIkkeSendteOppgaver();
    }
}
