package no.nav.veilarbaktivitet.person

import no.nav.common.client.pdl.PdlClient
import no.nav.common.client.utils.graphql.GraphqlRequestBuilder
import no.nav.common.client.utils.graphql.GraphqlResponse
import no.nav.common.client.utils.graphql.GraphqlUtils
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class EksternNavnService(val pdlClient: PdlClient) {

    val logger = LoggerFactory.getLogger(this::class.java)

    fun hentNavn(fnr: Person.Fnr): Navn {
        val graphqlRequest = GraphqlRequestBuilder<QueryVariables>("graphql/pdl/navnQuery.graphql")
            .buildRequest(QueryVariables(ident = fnr.get(), historikk = false))
        val navnResult = pdlClient.request(graphqlRequest, NavnResponse::class.java)
            .also { GraphqlUtils.logWarningIfError(it) }
        if(navnResult.errors?.isNotEmpty() == true) { throw RuntimeException("Feil ved kall til pdl") }
        val navnFraPdl = navnResult.data.hentPerson.navn
        return when {
            navnFraPdl.size == 1 -> navnFraPdl.first().tilNavn()
            navnFraPdl.size > 1 ->
                navnFraPdl.firstOrNull { it.metadata.master == NavnMaster.PDL }?.tilNavn() ?:
                navnFraPdl.firstOrNull { it.metadata.master == NavnMaster.FREG }?.tilNavn() ?:
                navnFraPdl.first().tilNavn()
            else -> throw RuntimeException("Personen har ikke navn")
        }
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
    val navn: List<PdlNavn>
)

data class NavnMetadata(
    val master: NavnMaster
)

enum class NavnMaster {
    FREG,
    PDL
}

data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String
) {
    fun tilFornavnMellomnavnEtternavn() = "${fornavn} ${mellomnavn?.plus(" ") ?: ""}${etternavn}"

}

data class PdlNavn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val metadata: NavnMetadata
) {
    fun tilNavn() = Navn(fornavn, mellomnavn, etternavn)
}

class NavnResponse: GraphqlResponse<NavnResponseData>()