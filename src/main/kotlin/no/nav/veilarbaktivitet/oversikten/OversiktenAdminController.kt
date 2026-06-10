package no.nav.veilarbaktivitet.oversikten

import no.nav.poao.dab.spring_auth.IAuthService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.*

@RestController
@RequestMapping("/api/oversikten/admin")
class OversiktenAdminController(
    private val authService: IAuthService,
    private val oversiktenService: OversiktenService,
    @Value("\${app.env.poaoAdminIdenter}")
    private val godkjenteAdminIdenter: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/republiser")
    fun republiserSisteMelding(@RequestBody request: RepubliserRequest): RepubliserResponse {
        sjekkTilgang()

        log.info("Republiserer siste melding for melding_key: ${request.meldingKey}")
        val nyId = oversiktenService.republiserSisteMelding(request.meldingKey)
        return RepubliserResponse(nyId, request.meldingKey)
    }

    private fun sjekkTilgang() {
        val veilederIdent = authService.getInnloggetVeilederIdent()
        val godkjente = godkjenteAdminIdenter.split(",").filter { it.isNotEmpty() }
        if (!godkjente.contains(veilederIdent.get())) {
            log.warn("Veileder ${veilederIdent.get()} har ikke tilgang til oversikten admin-endepunkt")
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Ikke tilgang til admin-endepunkt")
        }
    }

    data class RepubliserRequest(val meldingKey: UUID)
    data class RepubliserResponse(val nyId: Long, val meldingKey: UUID)
}

