package no.nav.veilarbaktivitet.arkivering

import lombok.extern.slf4j.Slf4j
import no.nav.poao.dab.spring_a2_annotations.auth.AuthorizeFnr
import no.nav.veilarbaktivitet.person.UserInContext
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Slf4j
@RestController
@RequestMapping("/api/arkivering")
class ArkiveringsController(private val userInContext: UserInContext) {

    @PostMapping
    @AuthorizeFnr(auditlogMessage = "arkivere aktivitetsplan og dialog")
    fun arkiverAktivitetsplanOgDialog() {
        val fnr = userInContext.fnr
        val navn = "Navn Navnesesn"



    }
}