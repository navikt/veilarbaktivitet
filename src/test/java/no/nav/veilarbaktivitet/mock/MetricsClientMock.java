package no.nav.veilarbaktivitet.mock;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;

import java.util.Map;

@Slf4j
public class MetricsClientMock implements MetricsClient {

    @Override
    public void report(Event event) {}

    @Override
    public void report(String name, Map<String, Object> fields, Map<String, String> tags, long l) {
        log.info("sender event %s Fields: %s Tags: %s".formatted(name, fields.toString(), tags.toString()));
    }

}
