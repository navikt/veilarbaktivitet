package no.nav.veilarbaktivitet.arkivering

import no.nav.common.json.JsonUtils
import no.nav.common.rest.client.RestUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class OrkivarClient(private val orkivarHttpClient: OkHttpClient) {

    @Value("\${orkivar.url}")
    lateinit var orkivarUrl: String

    fun hentPdfForForhaandsvisning(arkivPayload: ArkivPayload): ForhaandsvisningResult {
        val payload = lagRequestBody(arkivPayload)
        val request: Request = Request.Builder()
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(payload)
            .url(String.format("%s/forhaandsvisning", orkivarUrl))
            .build()

        val response = orkivarHttpClient.newCall(request).execute()

        return RestUtils.parseJsonResponse(response, ForhaandsvisningResult::class.java)
            .orElseThrow { RuntimeException("Kunne ikke hente PDF for forhåndsvisning") }
    }

    fun journalfor(arkivPayload: ArkivPayload): JournalføringResult {
        val payload = lagRequestBody(arkivPayload)
        val request: Request = Request.Builder()
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(payload)
            .url(String.format("%s/arkiver", orkivarUrl))
            .build()

        val response = orkivarHttpClient.newCall(request).execute()

        return RestUtils.parseJsonResponse(response, JournalføringResult::class.java)
            .orElseThrow { RuntimeException("Kunne ikke journalføre aktivitetsplan og dialog") }
    }

    private fun lagRequestBody(arkivPayload: ArkivPayload): RequestBody {
        return JsonUtils.toJson(arkivPayload).toRequestBody("application/json".toMediaTypeOrNull())
    }

    data class ForhaandsvisningResult(
        val pdf: ByteArray,
        val sistJournalført: LocalDateTime?
    )

    data class JournalføringResult(
        val sistJournalført: LocalDateTime
    )
}
