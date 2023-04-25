package no.nav.veilarbaktivitet.util;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static no.nav.common.utils.EnvironmentUtils.isProduction;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/logger")
public class Frontendlogger {
    private final MetricsClient metricsClient;

    @PostMapping("/event")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void skrivEventTilInflux(@RequestBody FrontendEvent event) {
        Event toInflux = new Event(event.name + ".event");
        if (event.getTags() != null) {
            event.getTags().forEach(toInflux::addTagToReport);
        }
        if (event.getFields() != null) {
            event.getFields().forEach(toInflux::addFieldToReport);
        }
        toInflux.getTags().put("environment", isProduction().orElse(false) ? "p" : "q1");

        metricsClient.report(toInflux);
    }

    @Data
    @Accessors(chain = true)
    static class FrontendEvent {
        @NonNull
        String name;
        Map<String, Object> fields;
        Map<String, String> tags;
    }
}
