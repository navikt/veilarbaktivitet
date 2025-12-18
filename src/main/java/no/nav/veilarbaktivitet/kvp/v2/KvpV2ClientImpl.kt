package no.nav.veilarbaktivitet.kvp.v2

import no.nav.common.client.utils.graphql.GraphqlRequest
import no.nav.common.client.utils.graphql.GraphqlRequestBuilder
import no.nav.common.client.utils.graphql.GraphqlResponse
import no.nav.common.json.JsonUtils
import no.nav.common.rest.client.RestUtils
import no.nav.common.types.identer.EnhetId
import no.nav.veilarbaktivitet.person.Person
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.*

@Service
class KvpV2ClientImpl(
    @Value("\${VEILARBOPPFOLGINGAPI_URL}")
    private var baseUrl: String,
    private val veilarboppfolgingHttpClient: OkHttpClient,
): KvpV2Client {

    override fun get(fnr: Person.Fnr): Optional<KontorSperre> {
        val uri = " ${baseUrl}/veilarboppfolging/api/graphql"

        val graphqlRequest: GraphqlRequest<*> = GraphqlRequestBuilder<KontorSperretEnhetQueryVariables>("graphql/veilarboppfolging/kontorSperreQuery.graphql")
            .buildRequest(KontorSperretEnhetQueryVariables(fnr = fnr.get()));
        val body: RequestBody = JsonUtils.toJson(graphqlRequest)
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(body)
            .url(uri)
            .build()

        try {
            veilarboppfolgingHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw RuntimeException("Feil ved henting av kontorsperreenhet fra veilarboppfolging (http status: ${response.code})")
                }
                val responseData = RestUtils.parseJsonResponseOrThrow(response, KontorSperretEnhetResponse::class.java)
                if(responseData.errors?.isNotEmpty() == true) {
                    throw RuntimeException("Feil ved henting av kontorsperreenhet fra veilarboppfolging (error i graphql response): ${responseData.errors.toString()}")
                }
                val kontorSperreEnhet = responseData?.data?.brukerStatus?.kontorSperre?.kontorId
                return if (kontorSperreEnhet == null) {
                    Optional.empty<KontorSperre>()
                } else {
                    Optional.of(KontorSperre(EnhetId(kontorSperreEnhet)))
                }
            }
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Feil ved kall mot ${request.url} - ${e.message}",
                e
            )
        }
    }

    override fun setBaseUrl(baseUrl: String) {
        this.baseUrl = baseUrl
    }

    private class KontorSperretEnhetResponse: GraphqlResponse<KontorSperreResult>()
}

data class KontorSperreDto(val kontorId: String? = null)
data class BrukerStatus(val kontorSperre: KontorSperreDto? = null)
data class KontorSperreResult(val brukerStatus: BrukerStatus? = null)

data class KontorSperre(val enhetId: EnhetId)