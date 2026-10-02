package no.nav.veilarbaktivitet.config

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.StatusCode
import org.springframework.stereotype.Component

/**
 * Tar tiden på henting av OBO-token (on-behalf-of) fra Azure AD / TokenX
 * før vi kaller en annen tjeneste, for å påvise hvor mye tid dette tar.
 *
 * Gir eget spenn i Tempo («hent OBO-token til <tjeneste>») og Prometheus-histogrammet
 * `obo_token_henting_seconds`. Treff i token-cachen gir korte målinger, faktisk kall mot
 * Azure AD / TokenX gir lange.
 */
@Component
class OboTokenTidtaker(private val meterRegistry: MeterRegistry) {

    private val tracer = GlobalOpenTelemetry.getTracer("veilarbaktivitet")

    fun <T> taTidPaaHentingAvToken(tilTjeneste: String, leverandor: String, hentToken: () -> T): T {
        val span = tracer.spanBuilder("hent OBO-token til $tilTjeneste")
            .setAttribute("obo_token.til_tjeneste", tilTjeneste)
            .setAttribute("obo_token.leverandor", leverandor)
            .startSpan()
        val sample = Timer.start(meterRegistry)
        var utfall = "ok"
        try {
            span.makeCurrent().use { return hentToken() }
        } catch (e: Exception) {
            utfall = "feil"
            span.recordException(e)
            span.setStatus(StatusCode.ERROR)
            throw e
        } finally {
            sample.stop(
                Timer.builder("obo.token.henting")
                    .description("Tid brukt på å hente OBO-token fra Azure AD / TokenX før kall til annen tjeneste")
                    .tag("til_tjeneste", tilTjeneste)
                    .tag("leverandor", leverandor)
                    .tag("utfall", utfall)
                    .publishPercentileHistogram()
                    .register(meterRegistry)
            )
            span.end()
        }
    }
}
