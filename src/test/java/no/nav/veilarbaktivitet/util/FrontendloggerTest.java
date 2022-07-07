package no.nav.veilarbaktivitet.util;

import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class FrontendloggerTest {
    @Captor
    ArgumentCaptor<Event> eventCaptor;
    MetricsClient metricsClient = mock(MetricsClient.class);
    Frontendlogger frontendlogger = new Frontendlogger(metricsClient);

    @Test
    public void loggerSkalLeggePaEnvironment() {
        HashMap<String, String> tags = new HashMap<>();
        tags.put("test", "test_value");
        frontendlogger.skrivEventTilInflux(new Frontendlogger.FrontendEvent().setName("test").setTags(tags));

        Event expected = new Event("test.event");
        tags.forEach(expected::addTagToReport);
        expected.addTagToReport("environment", "q1");

        verify(metricsClient, times(1)).report(eventCaptor.capture());
        assertEquals(expected.getTags(), eventCaptor.getValue().getTags());
        assertEquals(expected.getFields(), eventCaptor.getValue().getFields());
    }
}
