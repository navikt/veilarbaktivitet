package no.nav.veilarbaktivitet.config.kafka;

import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaHelsesjekOppdatererTest {

    @Mock
    private KafkaStringTemplate kafkaStringTemplate;
    @Mock
    private KafkaHelsesjekk kafkaHelsesjekk;

    String topicNavn = "portefoljeV1";

    @InjectMocks
    private KafkaHelsesjekOppdaterer kafkaHelsesjekOppdaterer;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(kafkaHelsesjekOppdaterer, "portefolgeTopic",
                topicNavn);
    }

    @Test
    void happyCase() {
        when(kafkaStringTemplate.partitionsFor(topicNavn)).thenReturn(List.of());
        kafkaHelsesjekOppdaterer.uppdateKafkaHelsesjek();
        verify(kafkaHelsesjekk).setIsHealty(true, "");
    }

    @Test
    void failureCase() {
        when(kafkaStringTemplate.partitionsFor(topicNavn)).thenThrow(new RuntimeException("Feilmelding"));
        kafkaHelsesjekOppdaterer.uppdateKafkaHelsesjek();
        verify(kafkaHelsesjekk).setIsHealty(false, "Feilmelding");
    }

}