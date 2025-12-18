package no.nav.veilarbaktivitet.norg2

import no.nav.common.client.norg2.Norg2Client
import no.nav.common.rest.client.RestUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class Norg2Client(private val norg2HttpClient: OkHttpClient) {


    @Value("\${norg2.url}")
    private lateinit var baseUrl: String;
    private val log = LoggerFactory.getLogger(Norg2Client::class.java)
    private var norgKontorCache = mapOf<String, NorgKontor>()

    private fun hentAlleEnheter(): Map<String, NorgKontor> {
        val url = "$baseUrl/api/v1/enhet"

        val request: Request = Request.Builder()
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .get()
            .url(url)
            .build()

        val response = norg2HttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            val responseData = RestUtils.parseJsonArrayResponse(response, NorgKontor::class.java).get()
            return responseData.associateBy { kontor -> kontor.enhetNr }
        } else {
            log.error("Feil ved henting av kontor fra norg")
            throw RuntimeException("Feil ved henting av kontor fra norg")
        }
    }

    private fun hentKontorFraCache(enhetId: String): NorgKontor? {
        val kontornavn = norgKontorCache[enhetId]

        return if (kontornavn != null) {
            kontornavn
        } else {
            norgKontorCache = hentAlleEnheter()
            norgKontorCache[enhetId]
        }
    }

    fun hentKontorNavn(enhetId: String): String {
        return hentKontorFraCache(enhetId)?.navn
            ?: throw RuntimeException("Finner ikke kontor med enhetId $enhetId")
    }
}

data class NorgKontor(
    val enhetNr: String,
    val navn: String,
)