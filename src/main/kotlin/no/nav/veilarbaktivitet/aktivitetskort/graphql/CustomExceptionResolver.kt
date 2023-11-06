package no.nav.veilarbaktivitet.aktivitetskort.graphql

import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.schema.DataFetchingEnvironment
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.graphql.execution.ErrorType
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException


@Component
class CustomExceptionResolver : DataFetcherExceptionResolverAdapter() {
    override fun resolveToSingleError(ex: Throwable, env: DataFetchingEnvironment): GraphQLError {
        val error = GraphqlErrorBuilder.newError()
            .path(env.executionStepInfo.path)
            .location(env.field.sourceLocation)
        return if (ex is ResponseStatusException) {
            when(ex.statusCode.value()) {
                403 -> error.errorType(ErrorType.FORBIDDEN).message("Ikke tilgang")
                401 -> error.errorType(ErrorType.UNAUTHORIZED).message("Ikke innlogget")
                else -> error.errorType(ErrorType.INTERNAL_ERROR).message("Ukjent feil")
            }.build()
        } else {
            return error.errorType(ErrorType.INTERNAL_ERROR).message("Ukjent feil").build()
        }
    }
}