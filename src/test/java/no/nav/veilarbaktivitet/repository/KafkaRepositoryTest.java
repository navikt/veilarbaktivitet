package no.nav.veilarbaktivitet.repository;

import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.kafka.KafkaTopics;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.repository.domain.FeiletKafkaMelding;
import no.nav.veilarbaktivitet.repository.domain.MeldingType;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KafkaRepositoryTest {

    private final static String TEST_AKTOR_ID = "123456";

    private JdbcTemplate db = LocalH2Database.getDb();

    private KafkaRepository kafkaRepository;

    @Before
    public void cleanup() {
        DbTestUtils.cleanupTestDb(db);
        kafkaRepository = new KafkaRepository(db);
    }

    @Test
    public void skal_lage_og_hente_feilet_produsert_kafka_melding() {
        String key = TEST_AKTOR_ID;
        String jsonPayload = "{}";

        kafkaRepository.lagreFeiletProdusertKafkaMelding(KafkaTopics.Topic.ENDRING_PA_AKTIVITET, key, jsonPayload);

        List<FeiletKafkaMelding> feiledeMeldinger = kafkaRepository.hentFeiledeKafkaMeldinger(MeldingType.PRODUCED);

        FeiletKafkaMelding melding = feiledeMeldinger.get(0);

        assertEquals(key, melding.getKey());
        assertEquals(jsonPayload, melding.getJsonPayload());
        assertEquals(KafkaTopics.Topic.ENDRING_PA_AKTIVITET, melding.getTopic());
        assertEquals(1, feiledeMeldinger.size());
    }

    @Test
    public void skal_slette_feilet_produsert_kafka_melding() {
        kafkaRepository.lagreFeiletProdusertKafkaMelding(KafkaTopics.Topic.ENDRING_PA_AKTIVITET, TEST_AKTOR_ID, "{}");
        List<FeiletKafkaMelding> feiledeMeldinger = kafkaRepository.hentFeiledeKafkaMeldinger(MeldingType.PRODUCED);
        kafkaRepository.slettFeiletKafkaMelding(feiledeMeldinger.get(0).getId());

        assertTrue(kafkaRepository.hentFeiledeKafkaMeldinger(MeldingType.PRODUCED).isEmpty());
    }

    @Test
    public void skal_lage_og_hente_feilet_konsumert_kafka_melding() {
        String key = TEST_AKTOR_ID;
        String jsonPayload = "{}";
        long offset = 42;

        kafkaRepository.lagreFeiletKonsumertKafkaMelding(KafkaTopics.Topic.KVP_AVSLUTTET, key, jsonPayload, offset);

        List<FeiletKafkaMelding> feiledeMeldinger = kafkaRepository.hentFeiledeKafkaMeldinger(MeldingType.CONSUMED);

        FeiletKafkaMelding melding = feiledeMeldinger.get(0);

        assertEquals(key, melding.getKey());
        assertEquals(KafkaTopics.Topic.KVP_AVSLUTTET, melding.getTopic());
        assertEquals(jsonPayload, melding.getJsonPayload());
        assertEquals(offset, melding.getOffset());
        assertEquals(1, feiledeMeldinger.size());
    }

    @Test
    public void skal_slette_feilet_konsumert_kafka_melding() {
        kafkaRepository.lagreFeiletKonsumertKafkaMelding(KafkaTopics.Topic.KVP_AVSLUTTET, TEST_AKTOR_ID, "{}", 42);
        List<FeiletKafkaMelding> feiledeMeldinger = kafkaRepository.hentFeiledeKafkaMeldinger(MeldingType.CONSUMED);
        kafkaRepository.slettFeiletKafkaMelding(feiledeMeldinger.get(0).getId());

        assertTrue(kafkaRepository.hentFeiledeKafkaMeldinger(MeldingType.CONSUMED).isEmpty());
    }

}
