package no.nav.veilarbaktivitet.aktivitet

import no.nav.poao.dab.spring_a2_annotations.auth.AuthorizeFnr
import no.nav.veilarbaktivitet.config.AktivitetResource
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/historikk")
class HistorikkController(private val historikkService: HistorikkService) {

    @GetMapping("/{id}")
    @AuthorizeFnr(auditlogMessage = "hent historikk for aktivitet", resourceIdParamName = "id", resourceType = AktivitetResource::class)
    fun hentHistorikk(@PathVariable("id") aktivitetId: Long): AktivitetHistorikk {
        return historikkService.hentHistorikk(aktivitetId)
    }
}