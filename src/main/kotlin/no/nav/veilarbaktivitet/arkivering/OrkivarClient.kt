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

    private val orkivarUrl = "$orkivarBaseUrl/orkivar"

    fun hentPdfForForhaandsvisning(forhåndsvisningPayload: ForhåndsvisningPayload): ForhaandsvisningResult {
        val request: Request = Request.Builder()
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(JsonUtils.toJson(forhåndsvisningPayload).toRequestBody("application/json".toMediaTypeOrNull()))
            .url("$orkivarUrl/forhaandsvisning")
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

    data class ForhaandsvisningResult(
        val pdf: ByteArray,
        val sistJournalført: LocalDateTime?
    )

    data class JournalføringResult(
        val sistJournalført: LocalDateTime
    )
}
