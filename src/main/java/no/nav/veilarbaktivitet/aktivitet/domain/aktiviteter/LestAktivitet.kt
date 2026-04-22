package no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData
import no.nav.veilarbaktivitet.avtalt_med_nav.Forhaandsorientering
import no.nav.veilarbaktivitet.person.Person
import java.time.ZonedDateTime
import java.util.*

data class AktivitetBaseData(
    // Selector field, kan velges av klient
    val id: Long,
    // Conflict check, kan velges av klient
    val versjon: Long,

    val opprettFelter: AktivitetBareOpprettFelter,
    val muterbareFelter: AktivitetMuterbareFelter,
    val genererteFelter: AktivitetGenererteFelter
)

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

sealed class LestAktivitet(val baseData: AktivitetBaseData) {
    val id get() = baseData.id
    val funksjonellId get() = baseData.genererteFelter.funksjonellId
    val versjon get() = baseData.versjon
    val aktorId get() = baseData.opprettFelter.aktorId
    val tittel get() = baseData.muterbareFelter.tittel
    val beskrivelse get() = baseData.muterbareFelter.beskrivelse
    val status get() = baseData.genererteFelter.status
    val avsluttetKommentar get() = baseData.genererteFelter.avsluttetKommentar
    val endretAvType get() = baseData.genererteFelter.sporingsData.endretAvType
    val fraDato get() = baseData.muterbareFelter.fraDato
    val tilDato get() = baseData.muterbareFelter.tilDato
    val lenke get() = baseData.muterbareFelter.lenke
    val opprettetDato get() = baseData.opprettFelter.opprettetDato
    val endretDato get() = baseData.genererteFelter.sporingsData.endretDato
    val endretAv get() = baseData.genererteFelter.sporingsData.endretAv
    val avtalt get() = baseData.genererteFelter.avtaltMedNav.erAvtalt
    val forhaandsorientering get() = baseData.genererteFelter.avtaltMedNav.forhaandsorientering
    val transaksjonsType get() = baseData.genererteFelter.transaksjonsType
    val historiskDato get() = baseData.genererteFelter.historiskDato
    val kontorsperreEnhetId get() = baseData.opprettFelter.kontorsperreEnhetId
    val lestAvBrukerForsteGang get() = baseData.genererteFelter.lestAvBrukerForsteGang
    val automatiskOpprettet get() = baseData.opprettFelter.automatiskOpprettet
    val malid get() = baseData.opprettFelter.malid
    val fhoId get() = baseData.genererteFelter.avtaltMedNav.fhoId
    val oppfolgingsperiodeId get() = baseData.opprettFelter.oppfolgingsperiodeId
}

