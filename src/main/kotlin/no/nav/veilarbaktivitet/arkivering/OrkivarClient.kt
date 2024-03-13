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
import java.util.*

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

    fun journalfor(arkivPayload: ArkivPayload) {
        val payload = lagRequestBody(arkivPayload)
        val request: Request = Request.Builder()
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(payload)
            .url(String.format("%s/arkiver", orkivarUrl))
            .build()

        val response = orkivarHttpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw RuntimeException("Kunne ikke journalføre aktiviteter og dialog")
        }
    }

    fun hentSistJournalført(oppfølgingsperiodeId: UUID): SistJournalførtResult {
        val request: Request = Request.Builder()
            .addHeader("Accept", "application/json")
            .url(String.format("%s/sistJournalfort/%s", orkivarUrl, oppfølgingsperiodeId))
            .build()

        val response = orkivarHttpClient.newCall(request).execute()

        return if (response.isSuccessful) {
            SistJournalførtSuccess(data = RestUtils.parseJsonResponse(response, ArkiveringsController.SistJournalførtDTO::class.java).get())
        } else {
            SistJournalførtFail(statuskode = response.code)
        }
    }

    private fun lagRequestBody(arkivPayload: ArkivPayload): RequestBody {
        return JsonUtils.toJson(arkivPayload).toRequestBody("application/json".toMediaTypeOrNull())
    }

    data class ForhaandsvisningResult(
        val pdf: ByteArray
    )

    sealed interface SistJournalførtResult
    data class SistJournalførtSuccess(val data: ArkiveringsController.SistJournalførtDTO): SistJournalførtResult
    data class SistJournalførtFail(val statuskode: Int): SistJournalførtResult
}
