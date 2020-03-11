package no.nav.fo.veilarbaktivitet.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Paths;
import java.util.Date;

import static java.nio.file.Files.newBufferedReader;
import static no.nav.common.utils.IdUtils.generateId;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetStatus.PLANLAGT;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.JOBBSOEKING;
import static no.nav.fo.veilarbaktivitet.kafka.KafkaDAO.TABLE_NAME;
import static no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants.SYSTEMUSER_PASSWORD;
import static no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants.SYSTEMUSER_USERNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked"})
public class KafkaServiceTest {

    private KafkaDAO dao;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setup() throws Exception {

        setProperties();

        JdbcDataSource ds = getInMemoryDatasource();
        RunScript.execute(ds.getConnection(), newBufferedReader(Paths.get("src/main/resources/db/migration/V1_28__feilede_kafka_meldinger.sql")));

        jdbcTemplate = new JdbcTemplate(ds);
        dao = new KafkaDAO(jdbcTemplate);

    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DROP TABLE " + TABLE_NAME);
    }

    @Test
    public void skal_sende_feilede_meldinger_paa_nytt() {
        dao.lagre(createMelding());
        assertThat(dao.antallFeiledeMeldinger()).isEqualTo(1);

        KafkaService kafkaService = new KafkaService(mock(KafkaProducer.class), dao);
        kafkaService.sendMelding(createMelding());
        assertThat(dao.antallFeiledeMeldinger()).isEqualTo(0);
    }

    @Test
    public void skal_legge_feilende_meldinger_i_database() {
        KafkaProducer<String, String> producer = mock(KafkaProducer.class);
        when(producer.send(any())).thenThrow(RuntimeException.class);
        KafkaService kafkaService = new KafkaService(producer, dao);

        kafkaService.sendMelding(createMelding());
        assertThat(dao.antallFeiledeMeldinger()).isEqualTo(1);
    }

    private KafkaAktivitetMelding createMelding() {

        return KafkaAktivitetMelding.builder()
                .navCallId(generateId())
                .aktivitetId("1")
                .aktorId("test_aktoer_id")
                .aktivitetStatus(PLANLAGT)
                .aktivitetType(JOBBSOEKING)
                .avtalt(true)
                .historisk(false)
                .endretDato(new Date())
                .fraDato(new Date())
                .tilDato(new Date(Long.MAX_VALUE))
                .build();
    }

    private JdbcDataSource getInMemoryDatasource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setUrl("jdbc:h2:mem:mock_db;MODE=Oracle;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    private void setProperties() {
        System.setProperty("KAFKA_TOPIC_AKTIVITETER", "mock_topic");
        System.setProperty("KAFKA_BROKERS_URL", "mock_url");
        System.setProperty(SYSTEMUSER_USERNAME, "mock_user");
        System.setProperty(SYSTEMUSER_PASSWORD, "mock_pwd");
    }

}
