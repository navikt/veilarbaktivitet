package no.nav.veilarbaktivitet.service;

import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.veilarbaktivitet.config.KafkaProperties;
import no.nav.veilarbaktivitet.domain.kafka.KafkaAktivitetMeldingV4;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static no.nav.common.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KafkaProducerServiceTest {

    @Test
    public void skal_sende_aktivitet_melding_med_header() {
        KafkaProperties properties = new KafkaProperties();
        properties.setEndringPaaAktivitetTopic("endring-pa-aktivitet");

        KafkaProducerClient<String, String> producerClient = mock(KafkaProducerClient.class);
        KafkaProducerService producerService = new KafkaProducerService(properties, producerClient);
        ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);

        when(producerClient.sendSync(captor.capture())).thenReturn(mockMetadata());

        KafkaAktivitetMeldingV4 aktivitetMelding = KafkaAktivitetMeldingV4.builder()
                .aktorId("1234")
                .build();

        String expectedAktivtetJson = "{\"aktivitetId\":null,\"version\":null,\"aktorId\":\"1234\",\"fraDato\":null,\"tilDato\":null,\"endretDato\":null,\"aktivitetType\":null,\"aktivitetStatus\":null,\"endringsType\":null,\"lagtInnAv\":null,\"avtalt\":false,\"historisk\":false}";

        producerService.sendAktivitetMelding(aktivitetMelding);

        ProducerRecord<String, String> sentRecord = captor.getValue();

        assertEquals(properties.getEndringPaaAktivitetTopic(), sentRecord.topic());
        assertEquals(expectedAktivtetJson, sentRecord.value());
        assertTrue(sentRecord.headers().headers(PREFERRED_NAV_CALL_ID_HEADER_NAME).iterator().next().value().length > 0);
    }

    private static RecordMetadata mockMetadata() {
        return new RecordMetadata(
                new TopicPartition("topic", 1),
                0L, 0, 0, 0L,0, 0
        );
    }

}
