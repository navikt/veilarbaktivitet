package no.nav.veilarbaktivitet.norg2

import no.nav.common.client.norg2.Norg2Client
import org.springframework.stereotype.Component

@Component
class Norg2Client(private val norg2HttpClient: Norg2Client) {

    private val hentAlleEnheterUrl = "http://norg2.org/norg2/api/v1/enhet"

    init {
        "http://norg2.org/norg2/api/v1/enhet"
    }

    private fun hentAlleEnheter()



}

/*

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
 */