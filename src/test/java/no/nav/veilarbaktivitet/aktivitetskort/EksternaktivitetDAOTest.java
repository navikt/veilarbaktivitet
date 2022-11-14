package no.nav.veilarbaktivitet.aktivitetskort;


import kafka.Kafka;
import lombok.Data;
import lombok.SneakyThrows;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.config.database.Database;
import no.nav.veilarbaktivitet.config.kafka.NavCommonKafkaConfig;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import shaded.com.google.common.collect.Streams;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static no.nav.veilarbaktivitet.aktivitetskort.IdentType.ARENAIDENT;

public class EksternaktivitetDAOTest {

    @Test
    @SneakyThrows
    public void test_json_serialisering() {
        JdbcTemplate jdbcTemplate = LocalH2Database.getPresistentDb();
        Database database = new Database(jdbcTemplate);
        AktivitetDAO aktivitetDAO = new AktivitetDAO(database, database.getNamedJdbcTemplate());

        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEksternAktivitet();
        AktivitetData opprettetAktivitetData = aktivitetDAO.opprettNyAktivitet(aktivitetData);

        AktivitetData aktivitet = aktivitetDAO.hentAktivitet(opprettetAktivitetData.getId());
        Assertions.assertThat(aktivitet).isEqualTo(opprettetAktivitetData);
    }

}
