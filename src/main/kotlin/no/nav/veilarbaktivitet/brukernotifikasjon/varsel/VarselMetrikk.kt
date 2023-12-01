package no.nav.veilarbaktivitet.brukernotifikasjon.varsel

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger

/**
 * brukernotifikasjon_mangler_kvittering teller bestilte varsler der vi ikke har fått kvittering.
 * I prometheus, bruk max() for å finne riktigste verdi, siden de forskjellige nodene kan ha ulike verdier.
 */
private const val BRUKERNOTIFIKASJON_MANGLER_KVITTERING = "brukernotifikasjon_mangler_kvittering"
@Component
class VarselMetrikk(meterRegistry: MeterRegistry?) {
    private val forsinkedeBestillinger = AtomicInteger()

    init {
        Gauge
            .builder(
                BRUKERNOTIFIKASJON_MANGLER_KVITTERING,
                forsinkedeBestillinger
            ) { obj: AtomicInteger -> obj.toDouble() }
            .register(meterRegistry)
    }

    fun countForsinkedeVarslerSisteDognet(antall: Int) {
        forsinkedeBestillinger.plain = antall
    }
}
