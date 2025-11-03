package no.nav.veilarbaktivitet.unleash

import io.getunleash.Unleash
import io.getunleash.UnleashContext
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import lombok.extern.slf4j.Slf4j
import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.auth.utils.CookieUtils
import okhttp3.internal.toHexString
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.*

@RestController
@Slf4j
@RequestMapping("/api/feature")
class FeatureController(
    private val unleash: Unleash,
    private val authContextHolder: AuthContextHolder,
    private val request: HttpServletRequest,
    private val response: HttpServletResponse,
) {
    private val UNLEASH_SESSION_ID_COOKIE_NAME = "UNLEASH_SESSION_ID"
    private val log = LoggerFactory.getLogger(javaClass)


    @GetMapping
    fun hentFeatures(@RequestParam("feature") features: List<String>): Map<String, Boolean> {
        val sessionId = CookieUtils.getCookie(UNLEASH_SESSION_ID_COOKIE_NAME, request)
            .map { it.value }
            .orElse(generateSessionId(response))

        // TODO Hva med eksternbruker?
        val erInternBruker = authContextHolder.erInternBruker()
        val navAnsattAzureId =
            if (erInternBruker) authContextHolder.idTokenClaims.get().getStringClaim("oid")
            else null

        val unleashContext = UnleashContext(
            navAnsattAzureId,
            sessionId,
            request.remoteAddr,
            emptyMap()
        )

        try {
            return features.associateWith { unleash.isEnabled(it, unleashContext) }
        } catch (e: Exception) {
            log.error("Feil under henting av features for unleash", e)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Feil under henting av features for unleash")
        }
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
