package no.nav.veilarbaktivitet.stilling_fra_nav;

import no.nav.veilarbaktivitet.avro.Arbeidssted;
import no.nav.veilarbaktivitet.avro.DelingAvCvRespons;
import no.nav.veilarbaktivitet.avro.ForesporselOmDelingAvCv;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.kvp.KvpService;
import no.nav.veilarbaktivitet.oppfolging_status.OppfolgingStatusClient;
import no.nav.veilarbaktivitet.oppfolging_status.OppfolgingStatusDTO;
import no.nav.veilarbaktivitet.service.AktivitetService;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OpprettForesporselOmDelingAvCvTest {

    public static final String BESTILLINGS_ID = "bestillingsId";
    public static final String AKTORID = "aktorid";
    public static final long AKTIVITET_ID = 1L;
    @Mock
    private KvpService kvpService;
    @Mock
    private AktivitetService aktivitetService;
    @Mock
    private DelingAvCvService delingAvCvService;
    @Mock
    private OppfolgingStatusClient oppfolgingStatusClient;
    @Mock
    private KafkaTemplate<String, DelingAvCvRespons> producerClient;
    private StillingFraNavProducerClient stillingFraNavProducerClient;

    @Captor
    private ArgumentCaptor<ProducerRecord<String, DelingAvCvRespons>> argumentCaptor;

    private OpprettForesporselOmDelingAvCv opprettForesporselOmDelingAvCv;
    private ForesporselOmDelingAvCv melding;

    @Before
    public void setup() {
        stillingFraNavProducerClient = new StillingFraNavProducerClient(producerClient, "topic.ut");
        opprettForesporselOmDelingAvCv = new OpprettForesporselOmDelingAvCv(kvpService, aktivitetService, delingAvCvService, oppfolgingStatusClient, stillingFraNavProducerClient);
        melding = createMelding();
    }

    @Test
    public void happyCase() {
        when(delingAvCvService.aktivitetAlleredeOpprettetForBestillingsId(BESTILLINGS_ID)).thenReturn(false);
        OppfolgingStatusDTO oppfolgingStatusDTO = OppfolgingStatusDTO.builder().underOppfolging(true).erManuell(false).build();
        when(oppfolgingStatusClient.get(Person.aktorId(AKTORID))).thenReturn(Optional.of(oppfolgingStatusDTO));
        when(kvpService.erUnderKvp(Person.aktorId(AKTORID))).thenReturn(false);
        when(aktivitetService.opprettAktivitet(any(), any(), any())).thenReturn(AKTIVITET_ID);

        opprettForesporselOmDelingAvCv.createAktivitet(melding);

        Mockito.verify(producerClient, atLeastOnce()).send((argumentCaptor.capture()));

        DelingAvCvRespons delingAvCvRespons = argumentCaptor.getValue().value();
        Assertions.assertThat(delingAvCvRespons.getBestillingsId()).isEqualTo(BESTILLINGS_ID);
        Assertions.assertThat(delingAvCvRespons.getAktivitetId()).isEqualTo(Long.toString(AKTIVITET_ID));
    }

    static ForesporselOmDelingAvCv createMelding() {
        return ForesporselOmDelingAvCv.newBuilder()
                .setAktorId(AKTORID)
                .setArbeidsgiver("arbeidsgiver")
                .setArbeidssteder(List.of(
                        Arbeidssted.newBuilder()
                                .setAdresse("adresse")
                                .setPostkode("1234")
                                .setKommune("kommune")
                                .setBy("by")
                                .setFylke("fylke")
                                .setLand("land").build(),
                        Arbeidssted.newBuilder()
                                .setAdresse("VillaRosa")
                                .setPostkode(null)
                                .setKommune(null)
                                .setBy(null)
                                .setFylke(null)
                                .setLand("spania").build()))
                .setBestillingsId(BESTILLINGS_ID)
                .setOpprettet(Instant.now())
                .setOpprettetAv("Z999999")
                .setCallId("callId")
                .setSoknadsfrist("10102021")
                .setStillingsId("stillingsId")
                .setStillingstittel("stillingstittel")
                .setSvarfrist(Instant.now().plus(5, ChronoUnit.DAYS))
                .build();
    }


}