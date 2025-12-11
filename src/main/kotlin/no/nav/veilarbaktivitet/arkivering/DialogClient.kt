package no.nav.veilarbaktivitet.arkivering

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.common.client.utils.graphql.GraphqlRequestBuilder
import no.nav.common.client.utils.graphql.GraphqlResponse
import no.nav.common.json.JsonUtils
import no.nav.common.rest.client.RestUtils
import no.nav.veilarbaktivitet.person.Person.Fnr
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.ZonedDateTime
import java.util.*

@Service
class DialogClient(private val dialogHttpClient: OkHttpClient) {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Value("\${veilarbdialog.url}")
    lateinit var dialogUrl: String;

    fun hentDialoger(fnr: Fnr): List<DialogTråd> {
        val graphqlRequest = GraphqlRequestBuilder<QueryVariables>("graphql/veilarbdialog/dialogQuery.graphql")
            .buildRequest(QueryVariables(fnr = fnr.get()))
        val body = JsonUtils.toJson(graphqlRequest).toRequestBody("application/json".toMediaType())

        val request: Request = Request.Builder()
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(body)
            .url("$dialogUrl/veilarbdialog/graphql")
            .build()

        return try {
            val response = dialogHttpClient.newCall(request).execute()
            val responseData: DialogResponse = RestUtils.parseJsonResponseOrThrow(response, DialogResponse::class.java)
            if(responseData.errors?.isNotEmpty() == true) { throw RuntimeException("Feil ved kall til veilarbdialog") }
            responseData.data?.dialoger ?: emptyList()
        } catch (e: Exception) {
            log.error("Feil ved henting av dialoger fra veilarbdialog", e)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Feil ved henting av dialoger")
        }
    }

    private data class QueryVariables(
        val fnr: String,
    )

    private class DialogResponse: GraphqlResponse<DialogData>()

    data class DialogData(
        val dialoger: List<DialogTråd>
    )

    data class DialogTråd(
        val id: String,
        val aktivitetId: String?,
        val overskrift: String?,
        val kontorsperreEnhetId: String?,
        @JsonProperty("oppfolgingsperiode")
        val oppfolgingsperiodeId: UUID,
        val opprettetDato: ZonedDateTime,
        @JsonProperty("henvendelser")
        val meldinger: List<Melding>,
        val egenskaper: List<Egenskap>,
        val erLestAvBruker: Boolean,
        val lestAvBrukerTidspunkt: ZonedDateTime?,
        val sisteDato: ZonedDateTime?,
    )

    data class Melding(
        val id: String,
        val dialogId: String,
        val avsender: Avsender,
        val avsenderId: String?,
        val sendt: ZonedDateTime,
        val lest: Boolean,
        val viktig: Boolean,
        val tekst: String
    )

    enum class Egenskap {
        ESKALERINGSVARSEL,
        PARAGRAF8 // forhåndsorientering, e-forvaltningsforskriften 8, ikke i bruk i veilarbdialog
    }

    enum class Avsender {
        BRUKER,
        VEILEDER
    }
}