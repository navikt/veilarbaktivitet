package no.nav.veilarbaktivitet.person

import no.nav.common.client.pdl.PdlClient
import no.nav.common.client.utils.graphql.GraphqlRequestBuilder
import no.nav.common.client.utils.graphql.GraphqlResponse
import org.springframework.stereotype.Service

@Service
class EksternNavnService(val pdlClient: PdlClient) {

    fun hentNavn(fnr: Person.Fnr): GraphqlResponse<NavnResponse> {
        val graphqlRequest = GraphqlRequestBuilder<QueryVariables>("/graphql/pdl/navnQuery.graphql")
            .buildRequest(QueryVariables(ident = fnr.get(), historikk = false))
        return pdlClient.request(graphqlRequest, GraphqlResponse<NavnResponse>().javaClass)
    }

}

data class QueryVariables(
    val ident: String,
    val historikk: Boolean
)

data class NavnResponse(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String
)