package no.nav.veilarbaktivitet.aktivitetskort.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import jakarta.validation.constraints.Size
import lombok.Singular
import no.nav.veilarbaktivitet.aktivitet.domain.Ident
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Attributt
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Etikett
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.LenkeSeksjon
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Oppgaver
import no.nav.veilarbaktivitet.aktivitetskort.util.ZonedOrNorwegianDateTimeDeserializer
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

data class Aktivitetskort(
    @JsonProperty(required = true)
    val id: UUID,
    @JsonProperty(required = true)
    val personIdent: String,
    @JsonProperty(required = true)
    @Size(max = 255, message = "Maks lengde på tittel i et aktivitetskort er 255")
    val tittel: String,
    val beskrivelse: String?,
    @JsonProperty(required = true)
    val aktivitetStatus: AktivitetskortStatus,
    val startDato: LocalDate? = null,
    val sluttDato: LocalDate? = null,
    @JsonProperty(required = true)
    val endretAv: Ident,
    @JsonProperty(required = true)
    @JsonDeserialize(using = ZonedOrNorwegianDateTimeDeserializer::class)
    val endretTidspunkt: ZonedDateTime,
    @JsonProperty(required = true)
    val avtaltMedNav: Boolean,
    val oppgave: Oppgaver? = null,
    @Singular("handling")
    val handlinger: List<LenkeSeksjon>? = null,
    @Singular("detalj")
    val detaljer: List<Attributt>? = null,
    @Singular("etikett")
    val etiketter: List<Etikett>? = null
) {}
