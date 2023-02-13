package no.nav.veilarbaktivitet.aktivitetskort;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import no.nav.veilarbaktivitet.aktivitetskort.dto.bestilling.AktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.service.UpsertActionResult;
import org.springframework.stereotype.Component;

@Component
public class AktivitetskortMetrikker {

    private final MeterRegistry meterRegistry;

    public static final String AKTIVITETSKORT_UPSERT = "aktivitetskort_upsert";
    public static final String AKTIVITETSKORT_FUNKSJONELL_FEIL = "aktivitetskort_funksjonell_feil";
    public static final String AKTIVITETSKORT_TEKNISK_FEIL = "aktivitetskort_teknisk_feil";


    public AktivitetskortMetrikker(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    void countAktivitetskortUpsert(AktivitetskortBestilling bestilling, UpsertActionResult upsertActionResult) {
        var type = bestilling.getAktivitetskortType().name();
        var source = bestilling.getSource();

        Counter.builder(AKTIVITETSKORT_UPSERT)
                .tag("type", type)
                .tag("source", source)
                .tag("action", upsertActionResult.name())
                .register(meterRegistry)
                .increment();
    }

    void countAktivitetskortFunksjonellFeil(String reason) {
        Counter.builder(AKTIVITETSKORT_FUNKSJONELL_FEIL)
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    void countAktivitetskortTekniskFeil() {
        Counter.builder(AKTIVITETSKORT_TEKNISK_FEIL)
                .register(meterRegistry)
                .increment();
    }

}
