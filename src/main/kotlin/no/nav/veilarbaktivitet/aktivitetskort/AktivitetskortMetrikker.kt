package no.nav.veilarbaktivitet.aktivitetskort

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.AktivitetskortBestilling
import no.nav.veilarbaktivitet.aktivitetskort.service.UpsertActionResult
import org.springframework.stereotype.Component

@Component
class AktivitetskortMetrikker(private val meterRegistry: MeterRegistry) {
    fun countAktivitetskortUpsert(bestilling: AktivitetskortBestilling, upsertActionResult: UpsertActionResult) {
        val type = bestilling.aktivitetskortType.name
        val source = bestilling.getSource()
        Counter.builder(AKTIVITETSKORT_UPSERT)
            .tag("type", type)
            .tag("source", source)
            .tag("action", upsertActionResult.name)
            .register(meterRegistry)
            .increment()
    }

    fun countAktivitetskortFunksjonellFeil(reason: String) {
        Counter.builder(AKTIVITETSKORT_FUNKSJONELL_FEIL)
            .tag("reason", reason)
            .register(meterRegistry)
            .increment()
    }

    fun countAktivitetskortTekniskFeil() {
        Counter.builder(AKTIVITETSKORT_TEKNISK_FEIL)
            .register(meterRegistry)
            .increment()
    }

    companion object {
        const val AKTIVITETSKORT_UPSERT = "aktivitetskort_upsert"
        const val AKTIVITETSKORT_FUNKSJONELL_FEIL = "aktivitetskort_funksjonell_feil"
        const val AKTIVITETSKORT_TEKNISK_FEIL = "aktivitetskort_teknisk_feil"
    }
}
