package no.nav.veilarbaktivitet.arkivering

import no.nav.common.json.JsonUtils
import no.nav.common.rest.client.RestUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class OrkivarClient(private val orkivarHttpClient: OkHttpClient, @Value("\${orkivar.url}") val orkivarBaseUrl: String) {

    private val orkivarUrl = "$orkivarBaseUrl"

    fun hentPdfForForhaandsvisningSendTilBruker(forhåndsvisningPayload: ForhåndsvisningPayload): ForhaandsvisningResult {
        val url = "$orkivarUrl/forhaandsvisning-send-til-bruker"
        return hentPdfForForhaandsvisning(forhåndsvisningPayload, url)
    }

    fun hentPdfForForhaandsvisning(forhåndsvisningPayload: ForhåndsvisningPayload): ForhaandsvisningResult {
        val url = "$orkivarUrl/forhaandsvisning"
        return hentPdfForForhaandsvisning(forhåndsvisningPayload, url)
    }

    private fun hentPdfForForhaandsvisning(forhåndsvisningPayload: ForhåndsvisningPayload, url: String): ForhaandsvisningResult {
        val request: Request = Request.Builder()
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(JsonUtils.toJson(forhåndsvisningPayload).toRequestBody("application/json".toMediaTypeOrNull()))
            .url(url)
            .build()

        val response = orkivarHttpClient.newCall(request).execute()

        return RestUtils.parseJsonResponse(response, ForhaandsvisningResult::class.java)
            .orElseThrow { RuntimeException("Kunne ikke hente PDF for forhåndsvisning") }
    }

    fun journalfor(arkivPayload: ArkivPayload): JournalføringResult {
        val request: Request = Request.Builder()
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(JsonUtils.toJson(arkivPayload).toRequestBody("application/json".toMediaTypeOrNull()))
            .url("$orkivarUrl/arkiver")
            .build()

        val response = orkivarHttpClient.newCall(request).execute()

        return RestUtils.parseJsonResponse(response, JournalføringResult::class.java)
            .orElseThrow { RuntimeException("Kunne ikke journalføre aktivitetsplan og dialog") }
    }

    fun sendTilBruker(sendTilBrukerPayload: SendTilBrukerPayload): SendTilBrukerResult {
        val request: Request = Request.Builder()
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(JsonUtils.toJson(sendTilBrukerPayload).toRequestBody("application/json".toMediaTypeOrNull()))
            .url("$orkivarUrl/send-til-bruker")
            .build()

        val response = orkivarHttpClient.newCall(request).execute()

        return when (response.isSuccessful) {
            true -> SendTilBrukerSuccess()
            false -> SendTilBrukerFail()
        }
    }

    data class ForhaandsvisningResult(
        val pdf: ByteArray,
        val sistJournalført: LocalDateTime?
    )

    data class JournalføringResult(
        val sistJournalført: LocalDateTime
    )

    sealed interface SendTilBrukerResult
    class SendTilBrukerSuccess: SendTilBrukerResult
    class SendTilBrukerFail: SendTilBrukerResult
}
