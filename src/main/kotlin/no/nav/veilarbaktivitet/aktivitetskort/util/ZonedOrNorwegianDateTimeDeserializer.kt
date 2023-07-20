package no.nav.veilarbaktivitet.aktivitetskort.util

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.deser.key.ZonedDateTimeKeyDeserializer
import java.io.IOException
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class ZonedOrNorwegianDateTimeDeserializer : JsonDeserializer<ZonedDateTime>() {
    @Throws(IOException::class)
    override fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): ZonedDateTime {
        val dateText = jsonParser.text
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
