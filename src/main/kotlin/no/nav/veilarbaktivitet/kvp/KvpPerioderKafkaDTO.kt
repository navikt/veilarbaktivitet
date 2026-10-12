package no.nav.veilarbaktivitet.kvp

import com.fasterxml.jackson.annotation.JsonAlias
import no.nav.veilarbaktivitet.util.NavCommonKafkaSerialized
import java.time.ZonedDateTime
import java.util.Optional

class KvpPerioderKafkaDTO : NavCommonKafkaSerialized {
    var uuid: String? = null
    var aktorId: String? = null
    var event: KvpPeriodeEvent? = null
    var enhetId: String? = null
    var startet: ZonedDateTime? = null
    var avsluttet: ZonedDateTime? = null

    @JsonAlias("startDato")
    var startDato: ZonedDateTime? = null

    @JsonAlias("sluttDato")
    var sluttDato: ZonedDateTime? = null

    fun hentStartTidspunkt(): Optional<ZonedDateTime> {
        return Optional.ofNullable(startet ?: startDato)
    }

    fun hentSluttTidspunkt(): Optional<ZonedDateTime> {
        return Optional.ofNullable(avsluttet ?: sluttDato)
    }
}
