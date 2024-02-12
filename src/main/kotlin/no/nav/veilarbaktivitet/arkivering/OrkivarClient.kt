package no.nav.veilarbaktivitet.arkivering

import no.nav.common.json.JsonUtils
import no.nav.common.rest.client.RestUtils
import no.nav.veilarbaktivitet.person.Person
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*

@Service
class OrkivarClient(private val orkivarHttpClient: OkHttpClient) {

    @Value("\${orkivar.url}")
    lateinit var orkivarUrl: String

    fun hentPdfForForhaandsvisning(
        fnr: Person.Fnr,
        navn: String,
        aktiviteterPayload: List<ArkivAktivitet>,
        dialogTråder: List<ArkivDialogtråd>
    ): ForhaandsvisningResult {
        val uri = String.format("%s/forhaandsvisning", orkivarUrl)
        val payload = JsonUtils.toJson(
            ArkivPayload(
                metadata = Metadata(navn, fnr.get()),
                aktiviteter = aktiviteterPayload
                    .groupBy { it.status },
                dialogtråder = dialogTråder
            )
        )
            .toRequestBody("application/json".toMediaTypeOrNull())
        val request: Request = Request.Builder()
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(payload)
            .url(uri)
            .build()

        val response = orkivarHttpClient.newCall(request).execute()

        return RestUtils.parseJsonResponse(response, ForhaandsvisningResult::class.java)
            .orElseThrow { RuntimeException("Kunne ikke hente PDF for forhåndsvisning") }
    }

    data class ForhaandsvisningResult(
        val uuid: UUID,
        val pdf: ByteArray
    )
}
