package no.nav.veilarbaktivitet.aktivitetskort;

import io.micrometer.core.instrument.MeterRegistry;
import net.javacrumbs.shedlock.core.LockProvider;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.arena.model.ArenaId;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringTemplate;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.testutils.AktivitetskortTestBuilder;
import no.nav.veilarbaktivitet.util.KafkaTestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;


public class AktivitetskortTestConsumerTest extends SpringBootTestBase {

    @Autowired
    KafkaTestService kafkaTestService;

    @Autowired
    UnleashClient unleashClient;

    @Value("${topic.inn.aktivitetskort}")
    String aktivitetskortTopic;

    @Autowired
    KafkaStringTemplate aktivitetskortProducer;

    @Autowired
    LockProvider lockProvider;

    @BeforeEach
    public void cleanupBetweenTests() {
        DbTestUtils.cleanupTestDb(jdbcTemplate);
    }

    @Test
    void aktivitetskorttestconsumer_skal_konsumere_og_finne_en_gjeldende_oppfolgingsperiode() {
        MockBruker mockBruker = MockNavService.createHappyBruker();

        UUID funksjonellId = UUID.randomUUID();

        String arenaTiltakskode = "MIDL";
        ArenaId arenaId = new ArenaId("ARENATA123");

        Aktivitetskort actual = AktivitetskortTestBuilder.ny(funksjonellId, AktivitetStatus.PLANLAGT, ZonedDateTime.now(), mockBruker);

        ArenaMeldingHeaders kontekst = new ArenaMeldingHeaders(arenaId, arenaTiltakskode);
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(List.of(actual), List.of(kontekst));
    }

}
