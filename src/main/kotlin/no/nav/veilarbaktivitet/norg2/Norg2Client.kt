package no.nav.veilarbaktivitet.norg2

import no.nav.common.client.norg2.Norg2Client
import no.nav.common.rest.client.RestUtils
import no.nav.common.types.identer.EnhetId
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class Norg2Client(private val norg2HttpClient: OkHttpClient) {

    private val hentAlleEnheterUrl = "http://norg2.org/norg2/api/v1/enhet"
    private val log = LoggerFactory.getLogger(Norg2Client::class.java)
    private var norgKontorCache = mapOf<EnhetId, NorgKontor>()

    init {
        norgKontorCache = hentAlleEnheter()
    }

    private fun hentAlleEnheter(): Map<EnhetId, NorgKontor> {
        val request: Request = Request.Builder()
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .get()
            .url(hentAlleEnheterUrl)
            .build()

        val response = norg2HttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            val responseData = RestUtils.parseJsonArrayResponse(response, NorgKontor::class.java).get()
            return responseData.associateBy { kontor -> kontor.enhetId }
        } else {
            log.error("Feil ved henting av kontor fra norg")
            throw RuntimeException("Feil ved henting av kontor fra norg")
        }
    }

    private fun hentKontorFraCache(enhetId: EnhetId): NorgKontor? {
        val kontornavn = norgKontorCache[enhetId]

        return if (kontornavn != null) {
            kontornavn
        } else {
            norgKontorCache = hentAlleEnheter()
            norgKontorCache[enhetId]
        }
    }

    fun hentKontorNavn(enhetId: EnhetId): String {
        return hentKontorFraCache(enhetId)?.navn
            ?: throw RuntimeException("Finner ikke kontor med enhetId $enhetId")
    }
}

data class NorgKontor(
    val enhetId: EnhetId,
    val navn: String,
)