package no.nav.veilarbaktivitet.arkivering

import lombok.extern.slf4j.Slf4j
import no.nav.poao.dab.spring_a2_annotations.auth.AuthorizeFnr
import no.nav.veilarbaktivitet.person.EksternNavnService
import no.nav.veilarbaktivitet.person.UserInContext
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@Slf4j
@RestController
@RequestMapping("/api/arkivering")
class ArkiveringsController(private val userInContext: UserInContext, private val orkivarClient: OrkivarClient, private val navnService: EksternNavnService) {

    @PostMapping
    @AuthorizeFnr(auditlogMessage = "arkivere aktivitetsplan og dialog")
    fun arkiverAktivitetsplanOgDialog() {
        val fnr = userInContext.fnr.get()
        val result = navnService.hentNavn(fnr)
        if(result.errors?.isNotEmpty() == true) { throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR) }
        val navn = result.data.hentPerson.navn.first().let { "${it.fornavn} ${it.mellomnavn?.plus(" ")}${it.etternavn}" }
        orkivarClient.arkiver(fnr, navn)
    }
}