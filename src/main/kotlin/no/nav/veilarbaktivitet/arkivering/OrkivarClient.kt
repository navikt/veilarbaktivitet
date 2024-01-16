package no.nav.veilarbaktivitet.arkivering

import no.nav.veilarbaktivitet.person.Person
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.apache.avro.data.Json
import org.springframework.stereotype.Service

@Service
class OrkivarClient(private val orkivarHttpClient: OkHttpClient, private val orkivarConfig: OrkivarConfig) {
    fun arkiver(fnr: Person.Fnr, navn: String) {
        val uri = String.format("%s/arkiver", orkivarConfig.url)
        val payload = Json.toString(ArkivPayload(metadata = Metadata(navn, fnr.get())))
            .toRequestBody("application/json".toMediaTypeOrNull())
        val request: Request = Request.Builder()
            .post(payload)
            .url(uri)
            .build()
        orkivarHttpClient.newCall(request).execute()
    }
}
