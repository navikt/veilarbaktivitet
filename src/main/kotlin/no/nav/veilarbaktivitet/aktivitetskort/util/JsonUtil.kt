package no.nav.veilarbaktivitet.aktivitetskort.util

import tools.jackson.core.JsonToken
import tools.jackson.core.ObjectReadContext
import tools.jackson.core.json.JsonFactory

object JsonUtil {
    private val jsonFactory: JsonFactory = JsonFactory.builder().build()

    @JvmStatic
    fun extractStringPropertyFromJson(propertyName: String, json: String): String? {
        var result: String? = null
        val parser = jsonFactory.createParser(ObjectReadContext.empty(), json)

        fun isEnd(token: JsonToken, level: Int): Boolean {
            return token == JsonToken.END_OBJECT && level == 0
        }

        parser.use {
            var level = 0
            do {
                val nextToken = it.nextToken()
                when (nextToken) {
                    JsonToken.END_OBJECT -> { level -= 1 }
                    JsonToken.START_OBJECT -> { level += 1 }
                    JsonToken.VALUE_STRING -> {
                        val fieldName = it.currentName()
                        if (propertyName == fieldName) {
                            result = it.valueAsString
                            break
                        }
                    }
                    else -> continue
                }

            } while (!isEnd(nextToken, level))
        }
        return result
    }
}
