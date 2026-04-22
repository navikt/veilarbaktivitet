package no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData
import no.nav.veilarbaktivitet.avtalt_med_nav.Forhaandsorientering
import no.nav.veilarbaktivitet.person.Person
import java.time.ZonedDateTime
import java.util.*

/**
* Felter som kan endres på aktiviteter
*/
class AktivitetMuterbareFelter(
    val tittel: String,
    val beskrivelse: String?,
    val fraDato: Date?,
    val tilDato: Date?,
    val lenke: String?,
)

/**
* Disse feltene settes bare ved opprettelse
*/
class AktivitetBareOpprettFelter(
    val aktorId: Person.AktorId,
    val aktivitetType: AktivitetTypeData,
    val status: AktivitetStatus,
    val kontorsperreEnhetId: String?, // Tror denne bør være konsistent alltid
    val malid: String?,
    val opprettetDato: ZonedDateTime,
    val automatiskOpprettet: Boolean,
    val oppfolgingsperiodeId: UUID, // Foreløpig bare en gang
)

/**
* Disse feltene kan aldri endres basert på (manuell) bruker-input
*/
class AktivitetGenererteFelter(
    val funksjonellId: UUID,
    val transaksjonsType: AktivitetTransaksjonsType,
    val historiskDato: Date,
    val sporingsData: SporingsData,

    val avtaltMedNav: AvtaltMedNavFelter,

    /**
     * Settes når bruker leser en aktivitet via
     * @see [no.nav.veilarbaktivitet.aktivitet.AktivitetAppService.hentAktivitet]
     */
    val lestAvBrukerForsteGang: Date?,
    /**
    * `status` og `avsluttetKommentar` settes via @see [no.nav.veilarbaktivitet.aktivitet.AktivitetsplanController.oppdaterStatus]
     * utendom for eksternaktiviteter (akaas)
    */
    val status: AktivitetStatus,
    val avsluttetKommentar: String?,
) {
    /**
     * Settes via @see [no.nav.veilarbaktivitet.avtalt_med_nav.AvtaltMedNavService.opprettFHO]
     * utendom for eksternaktiviteter (akaas)
     */
    data class AvtaltMedNavFelter(
        val erAvtalt: Boolean,
        val fhoId: String?,
        val forhaandsorientering: Forhaandsorientering?,
    )
}
