package no.nav.veilarbaktivitet.arkivering

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.common.rest.client.RestUtils
import no.nav.veilarbaktivitet.person.Person.Fnr
import okhttp3.OkHttpClient
import okhttp3.Request
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

    fun hentDialogerUtenKontorsperre(fnr: Fnr): List<DialogTråd> {
        val uri = "$dialogUrl/veilarbdialog/api/dialog?fnr=${fnr.get()}&ekskluderDialogerMedKontorsperre=true"

        val request: Request = Request.Builder()
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .get()
            .url(uri)
            .build()

        return try {
            val response = dialogHttpClient.newCall(request).execute()
            RestUtils.parseJsonResponseArrayOrThrow(response, DialogTråd::class.java)
        } catch (e: Exception) {
            log.error("Feil ved henting av dialoger fra veilarbdialog", e)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Feil ved henting av dialoger")
        }
    }

    data class DialogTråd(
        val id: String,
        val aktivitetId: String?,
        val overskrift: String?,
        @JsonProperty("oppfolgingsperiode")
        val oppfolgingsperiodeId: UUID,
        @JsonProperty("henvendelser")
        val meldinger: List<Melding>,
        val egenskaper: List<Egenskap>,
        val erLestAvBruker: Boolean,
        val lestAvBrukerTidspunkt: ZonedDateTime?,
    )

    data class Melding(
        val id: String,
        val dialogId: String,
        val avsender: Avsender,
        val avsenderId: String,
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