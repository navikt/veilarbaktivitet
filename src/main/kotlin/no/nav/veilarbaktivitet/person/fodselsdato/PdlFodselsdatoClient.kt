package no.nav.veilarbaktivitet.person.fodselsdato

import java.time.LocalDate
import no.nav.common.client.pdl.PdlClient
import no.nav.common.client.utils.graphql.GraphqlRequestBuilder
import no.nav.common.client.utils.graphql.GraphqlResponse
import no.nav.common.client.utils.graphql.GraphqlUtils
import no.nav.common.types.identer.Fnr
import no.nav.veilarbaktivitet.person.QueryVariables
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

data class Foedselsdato(
    val foedselsdato: String,
) {
    fun toLocalDate(): LocalDate {
        return LocalDate.parse(foedselsdato)
    }
}

data class HentFodselsdato(
    val foedselsdato: List<Foedselsdato>,
)

data class HentFodselsdatoQuery(
    val hentPerson: HentFodselsdato,
)

class HentFodselsdatoGraphqlWrapper: GraphqlResponse<HentFodselsdatoQuery>()

@Service
class PdlFodselsdatoClient(val pdlClient: PdlClient) {
    val logger = LoggerFactory.getLogger(PdlFodselsdatoClient::class.java)

    fun erUnder18(fnr: Fnr): Boolean {
        val fodselsdato = hentFodselsdato(fnr)
        return fodselsdato?.let { erUnder18Aar(it) } ?: throw IllegalArgumentException("Kunne ikke hente fødselsdato fra PDL")
    }

    private fun hentFodselsdato(fnr: Fnr): LocalDate? {
        val graphqlRequest = GraphqlRequestBuilder<QueryVariables>("graphql/pdl/fodselsdatoQuery.graphql")
            .buildRequest(QueryVariables(ident = fnr.get(), historikk = false))
        val result = pdlClient.request(graphqlRequest, HentFodselsdatoGraphqlWrapper::class.java)
            .also { GraphqlUtils.logWarningIfError(it) }

        if(result.errors?.isNotEmpty() == true) {
            throw kotlin.RuntimeException("Feil ved henting av fødselsdato fra pdl ${result?.errors.toString()}")
        }

        return result.data.hentPerson.foedselsdato.firstOrNull()?.toLocalDate()
    }

    private fun erUnder18Aar(fodselsdato: LocalDate): Boolean {
        return fodselsdato.isAfter(LocalDate.now().minusYears(18))
    }
}
