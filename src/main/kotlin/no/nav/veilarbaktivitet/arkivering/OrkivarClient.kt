package no.nav.veilarbaktivitet.arkivering

import no.nav.common.json.JsonUtils
import no.nav.veilarbaktivitet.person.Person
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class OrkivarClient(private val orkivarHttpClient: OkHttpClient) {

    @Value("\${orkivar.url}")
    lateinit var orkivarUrl: String

    fun arkiver(fnr: Person.Fnr, navn: String, aktiviteterPayload: List<ArkivAktivitet>) {
        val uri = String.format("%s/arkiver", orkivarUrl)
        val payload = JsonUtils.toJson(
                ArkivPayload(
                    metadata = Metadata(navn, fnr.get()),
                    aktiviteter = aktiviteterPayload
                        .groupBy { it.status }

                )
            )
            .toRequestBody("application/json".toMediaTypeOrNull())
        val request: Request = Request.Builder()
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(payload)
            .url(uri)
            .build()
        orkivarHttpClient.newCall(request).execute()
    }
}
