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
    public void loggerSkalLeggePaTagsOgFields() {
        HashMap<String, String> tags = new HashMap<>();
        tags.put("test", "test_value");
        HashMap<String, Object> fields = new HashMap<>();
        fields.put("fields", "test_value");
        fields.put("success", true);
        fields.put("value", 1);
        frontendlogger.skrivEventTilInflux(new Frontendlogger.FrontendEvent("test").setTags(tags).setFields(fields));

        Event expected = new Event("test.event");
        tags.forEach(expected::addTagToReport);
        fields.forEach(expected::addFieldToReport);
        expected.addTagToReport("environment", "q1");

        verify(metricsClient, times(1)).report(eventCaptor.capture());
        assertEquals(expected.getTags(), eventCaptor.getValue().getTags());
        assertEquals(expected.getFields(), eventCaptor.getValue().getFields());
    }

    @Test
    public void loggerSkalLeggeHandtereNull() {
        frontendlogger.skrivEventTilInflux(new Frontendlogger.FrontendEvent("test"));

        Event expected = new Event("test.event");
        expected.addTagToReport("environment", "q1");

        verify(metricsClient, times(1)).report(eventCaptor.capture());
        assertEquals(expected.getTags(), eventCaptor.getValue().getTags());
        assertEquals(expected.getFields(), eventCaptor.getValue().getFields());
    }
}
