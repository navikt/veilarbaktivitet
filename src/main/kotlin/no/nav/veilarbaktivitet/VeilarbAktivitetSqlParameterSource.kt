package no.nav.veilarbaktivitet

import org.springframework.jdbc.core.namedparam.AbstractSqlParameterSource
import java.time.ZonedDateTime

class VeilarbAktivitetSqlParameterSource: AbstractSqlParameterSource() {

    private val values = LinkedHashMap<String, Any?>()

    fun addValue(paramName: String, value: Any?): VeilarbAktivitetSqlParameterSource {
        val castedValue = when {
            value is Boolean -> if (value) 1 else 0
            value is ZonedDateTime -> value.toOffsetDateTime()
            else -> value
        }

        values[paramName] = castedValue
        return this
    }

    override fun hasValue(paramName: String): Boolean {
        return values.containsValue(paramName)
    }

    override fun getValue(paramName: String): Any? {
        return values[paramName]
    }
}