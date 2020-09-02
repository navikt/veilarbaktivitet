package no.nav.veilarbaktivitet.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.leaderelection.LeaderElectionClient;
import no.nav.veilarbaktivitet.db.dao.MoteSmsDAO;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jms.core.JmsTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MoteSMSServiceTest {

    @Mock
    private JmsTemplate varselQueue;

    @Mock
    private MoteSmsDAO moteSmsDAO;

    @Mock
    private UnleashService unleash;

    @Mock
    private LeaderElectionClient leaderElectionClient;

    @Mock
    private MeterRegistry registry;

    @InjectMocks
    private MoteSMSService moteSMSService;

    @BeforeClass
    public static void systemProp() {
        System.setProperty("AKTIVITETSPLAN_URL", "aktivitesplan");
    }

    @BeforeEach
    public void setupMock() {
        when(registry.counter(any())).thenReturn(Mockito.mock(Counter.class));
        when(registry.gauge(any(), any())).thenReturn(Mockito.mock(Gauge.class));
        when(leaderElectionClient.isLeader()).thenReturn(true);
        when(unleash.isEnabled("veilarbaktivitet.motesms")).thenReturn(true);
        when(moteSmsDAO.hentIkkeAvbrutteMoterMellom(any(), any())).thenReturn(List.of());
        System.setProperty("AKTIVITETSPLAN_URL", "kake");
        
    }

    @Test
    public void skalIkkeFeileMedTomListe() {
        moteSMSService.sendSms();
    }

    //TODO skriv tester/refaktorer for testing.



}
