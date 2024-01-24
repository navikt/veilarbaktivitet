package no.nav.veilarbaktivitet.arkivering

import no.nav.poao.dab.spring_a2_annotations.auth.AuthorizeFnr
import no.nav.veilarbaktivitet.aktivitet.AktivitetAppService
import no.nav.veilarbaktivitet.arkivering.mapper.toArkivPayload
import no.nav.veilarbaktivitet.person.EksternNavnService
import no.nav.veilarbaktivitet.person.UserInContext
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/arkivering")
class ArkiveringsController(
    private val userInContext: UserInContext,
    private val orkivarClient: OrkivarClient,
    private val navnService: EksternNavnService,
    private val appService: AktivitetAppService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping
    @AuthorizeFnr(auditlogMessage = "arkivere aktivitetsplan og dialog")
    fun arkiverAktivitetsplanOgDialog() {
        val fnr = userInContext.fnr.get()
        val result = navnService.hentNavn(fnr)
        if(result.errors?.isNotEmpty() == true) { throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR) }
        val navn = result.data.hentPerson.navn.first().tilFornavnMellomnavnEtternavn()
        val aktiviteterPayload = appService.hentAktiviteterForIdentMedTilgangskontroll(fnr)
            .map { it.toArkivPayload() }
        orkivarClient.arkiver(fnr, navn, aktiviteterPayload)
    }
}
