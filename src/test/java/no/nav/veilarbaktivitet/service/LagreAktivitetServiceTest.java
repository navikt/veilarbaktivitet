package no.nav.veilarbaktivitet.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.kafka.KafkaService;
import no.nav.veilarbaktivitet.kafka.KafkaServiceImpl;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class LagreAktivitetServiceTest {

    @Mock
    private AktivitetDAO aktivitetDAO;

    @Mock
    private Producer<String, String> producer;

    @Mock
    private Future<RecordMetadata> shouldOnlySupportGet;

    @BeforeClass
    public static void beforeClass() {

        // PAIN: getRequiredProperty(...)
        System.setProperty("KAFKA_TOPIC_AKTIVITETER", "n/a");
        System.setProperty("KAFKA_BROKERS_URL", "n/a");

    }

    @Test
    public void lagreAktivitet() {

        when(producer.send(any())).thenReturn(waitBeforeReturning(shouldOnlySupportGet));

        KafkaService kafkaService = new KafkaServiceImpl(producer);
        MeterRegistry meterRegistry = new SimpleMeterRegistry();

        LagreAktivitetService service = new LagreAktivitetService(aktivitetDAO, kafkaService, meterRegistry);
        service.lagreAktivitet(AktivitetData.builder().build());

        Timer timer = meterRegistry.find("my.timer").timer();
        assertThat(timer).isNotNull();
        log.info("Timer is now at {}ms total time", timer.totalTime(TimeUnit.MILLISECONDS));
        assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThan(0);

        Counter counter = meterRegistry.find("my.counter").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1);

        // Let's repeat that counter action...
        service.lagreAktivitet(AktivitetData.builder().build());

        counter = meterRegistry.find("my.counter").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2);

    }

    private static Future<RecordMetadata> waitBeforeReturning(Future<RecordMetadata> value) {
        try {
            log.info("Zzz...");
            Thread.sleep(500);
        }catch (InterruptedException e) {
            log.warn("We've been interrupted!");
        }
        return value;
    }

}
