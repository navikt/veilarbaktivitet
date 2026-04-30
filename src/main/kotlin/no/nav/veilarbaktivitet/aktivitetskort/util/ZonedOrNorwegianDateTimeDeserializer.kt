package no.nav.veilarbaktivitet.aktivitetskort.util

import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import java.io.IOException
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer
import tools.jackson.databind.ext.javatime.deser.key.ZonedDateTimeKeyDeserializer

class ZonedOrNorwegianDateTimeDeserializer : ValueDeserializer<ZonedDateTime>() {
    @Throws(IOException::class)
    override fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): ZonedDateTime {
        val dateText = jsonParser.string
        val localDateTimeRegex = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,6})?$"
        return if (dateText.matches(localDateTimeRegex.toRegex())) {
            localDateTimeDeserializer.deserialize(
                jsonParser,
                deserializationContext
            ).atZone(ZoneId.of("Europe/Oslo"))
        } else {
            zonedDateTimeKeyDeserializer.deserializeKey(
                dateText,
                deserializationContext
            ) as ZonedDateTime
        }
    }

    companion object {
        private val zonedDateTimeKeyDeserializer = ZonedDateTimeKeyDeserializer.INSTANCE
        private val localDateTimeDeserializer = LocalDateTimeDeserializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
}
