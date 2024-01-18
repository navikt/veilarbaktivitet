package no.nav.veilarbaktivitet.person

import no.nav.common.client.pdl.PdlClient
import no.nav.common.client.utils.graphql.GraphqlRequestBuilder
import no.nav.common.client.utils.graphql.GraphqlResponse
import no.nav.common.client.utils.graphql.GraphqlUtils
import org.springframework.stereotype.Service

@Service
class EksternNavnService(val pdlClient: PdlClient) {

    fun hentNavn(fnr: Person.Fnr): NavnResponse {
        val graphqlRequest = GraphqlRequestBuilder<QueryVariables>("graphql/pdl/navnQuery.graphql")
            .buildRequest(QueryVariables(ident = fnr.get(), historikk = false))
        return pdlClient.request(graphqlRequest, NavnResponse::class.java)
            .also { GraphqlUtils.logWarningIfError(it) }
    }

}

data class QueryVariables(
    val ident: String,
    val historikk: Boolean
)

data class NavnResponseData(
    val hentPerson: HentPerson
)

data class HentPerson(
    val navn: List<Navn>
)

data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String
)

class NavnResponse: GraphqlResponse<NavnResponseData>()