package no.nav.veilarbaktivitet.aktivitetskort.util

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonToken
import java.io.IOException

object JsonUtil {
    @JvmStatic
    @Throws(IOException::class)
    fun extractStringPropertyFromJson(propertyName: String, json: String): String? {
        var result: String? = null
        val parser = JsonFactory().createParser(json)
        parser.use { parser ->
            parser.nextToken()
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                val fieldName = parser.currentName
                if (propertyName == fieldName && parser.nextToken() == JsonToken.VALUE_STRING) {
                    result = parser.valueAsString
                    break
                }
            }
        }
        return result
    }
}
