package no.nav.veilarbaktivitet.person

import no.nav.common.client.pdl.PdlClient
import no.nav.common.client.utils.graphql.GraphqlRequestBuilder
import no.nav.common.client.utils.graphql.GraphqlResponse
import no.nav.common.client.utils.graphql.GraphqlUtils
import no.nav.veilarbaktivitet.person.NavnMaster.FREG
import no.nav.veilarbaktivitet.person.NavnMaster.PDL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EksternNavnService(val pdlClient: PdlClient) {

    val logger = LoggerFactory.getLogger(this::class.java)

    fun hentNavn(fnr: Person.Fnr): Navn {
        val graphqlRequest = GraphqlRequestBuilder<QueryVariables>("graphql/pdl/navnQuery.graphql")
            .buildRequest(QueryVariables(ident = fnr.get(), historikk = false))
        val navnResult = pdlClient.request(graphqlRequest, NavnResponse::class.java)
            .also { GraphqlUtils.logWarningIfError(it) }
        if(navnResult.errors?.isNotEmpty() == true) { throw RuntimeException("Feil ved kall til pdl") }
        return navnResult.data.hentPerson.hentNavn()
    }
}

data class QueryVariables(
    val ident: String,
    val historikk: Boolean
)

data class NavnResponseData(
    val hentPerson: PdlPerson
)

data class PdlPerson(
    private val navn: List<PdlNavn>
) {
    fun hentNavn(): Navn = when {
        navn.size == 1 -> navn.first().tilNavn()
        navn.size > 1 ->
            navn.firstOrNull { it.metadata.master == PDL }?.tilNavn() ?:
            navn.first { it.metadata.master == FREG }.tilNavn()
            else -> throw IllegalStateException("Personen har ikke navn - dette skal aldri skje")
        }
}

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