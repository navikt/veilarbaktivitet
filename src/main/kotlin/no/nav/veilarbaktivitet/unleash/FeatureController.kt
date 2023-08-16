package no.nav.veilarbaktivitet.unleash

import io.getunleash.Unleash
import io.getunleash.UnleashContext
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import lombok.extern.slf4j.Slf4j
import no.nav.common.auth.utils.CookieUtils
import no.nav.common.types.identer.NavIdent
import no.nav.poao.dab.spring_auth.IAuthService
import okhttp3.internal.toHexString
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@Slf4j
@RequestMapping("/api/feature")
class FeatureController(
    private val unleash: Unleash,
    private val authService: IAuthService,
    private val request: HttpServletRequest,
    private val response: HttpServletResponse,
) {
    private val UNLEASH_SESSION_ID_COOKIE_NAME = "UNLEASH_SESSION_ID"

    @GetMapping
    fun hentFeatures(@RequestParam("feature") features: List<String>): Map<String, Boolean> {
        val sessionId = CookieUtils.getCookie(UNLEASH_SESSION_ID_COOKIE_NAME, request)
            .map { it.value }
            .orElse(generateSessionId(response))

        val id = authService.getLoggedInnUser().takeIf { it is NavIdent }?.get()

        val unleashContext = UnleashContext(
            id,
            sessionId,
            request.remoteAddr,
            emptyMap()
        )
        return features.associateWith { unleash.isEnabled(it, unleashContext) }
    }

    private fun generateSessionId(httpServletResponse: HttpServletResponse): String {
        val uuid = UUID.randomUUID()
        val sessionId = uuid.mostSignificantBits.toHexString() + uuid.leastSignificantBits.toHexString()
        val cookie = Cookie(UNLEASH_SESSION_ID_COOKIE_NAME, sessionId)
        cookie.path = "/"
        cookie.maxAge = -1
        httpServletResponse.addCookie(cookie)
        return sessionId
    }
}
