package no.nav.veilarbaktivitet.oppfolging.siste_periode;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class GjeldendePeriodeMetrikk {
    public static final String GJELDENDE_TAG = "gjeldende";
    private final MeterRegistry meterRegistry;

    private static final String EKSTERN_GJELDENDE_OPPFOLGINGSPERIODE = "ekstern_gjeldende_oppfolgingsperiode";

    public GjeldendePeriodeMetrikk(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        meterRegistry.counter(EKSTERN_GJELDENDE_OPPFOLGINGSPERIODE, GJELDENDE_TAG);
    }

    public void tellKallTilEksternOppfolgingsperiode(boolean harGjeldende) {
        Counter.builder(EKSTERN_GJELDENDE_OPPFOLGINGSPERIODE)
                .tag(GJELDENDE_TAG, "" + harGjeldende)
                .register(meterRegistry)
                .increment();
    }
}
