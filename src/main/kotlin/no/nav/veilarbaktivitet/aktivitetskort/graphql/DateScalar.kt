package no.nav.veilarbaktivitet.aktivitetskort.graphql

import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.CoercingParseValueException
import graphql.schema.GraphQLScalarType
import no.nav.veilarbaktivitet.util.DateUtils
import java.time.ZoneOffset
import java.util.*

object DateScalar {
    val DATESCALAR = GraphQLScalarType.newScalar()
        .name("Date")
        .description("A custom scalar that handles zonedDateTime")
        .coercing(object : Coercing<Any?, String> {
            override fun serialize(dataFetcherResult: Any): String {
                return serializeDate(dataFetcherResult)
            }
            override fun parseValue(input: Any): Any {
                return parseDateFromVariable(input)
            }
            override fun parseLiteral(input: Any): Any {
                return parseDateFromAstLiteral(input)
            }
        }).build()

    fun parseDateFromAstLiteral(input: Any): Any {
        return when (input) {
            is StringValue -> DateUtils.dateFromISO8601(input.value)
            else -> throw CoercingParseValueException("Failed to parse input in parseZonedDateFromAstLiteral")
        }
    }
    fun parseDateFromVariable(input: Any): Any {
        return when {
            input is String -> DateUtils.dateFromISO8601(input)
            else -> throw CoercingParseValueException("Failed to parse input in parseZonedDateFromVariable")
        }
    }
    fun serializeDate(dataFetcherResult: Any): String {
        return when (dataFetcherResult) {
            is Date -> DateUtils.iso8601Fromdate(dataFetcherResult, ZoneOffset.systemDefault())
            else -> throw CoercingParseValueException("Failed to parse input in serializeDate")
        }
    }
}