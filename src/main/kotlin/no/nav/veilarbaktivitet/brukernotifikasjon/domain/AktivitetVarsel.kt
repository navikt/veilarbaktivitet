package no.nav.veilarbaktivitet.brukernotifikasjon.domain

import no.nav.veilarbaktivitet.brukernotifikasjon.VarselStatus
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType
import no.nav.veilarbaktivitet.person.Person
import java.util.*

data class AktivitetVarsel(
    val aktivitetId: Long,
    val aktitetVersion: Long,
    val aktorId: Person.AktorId,
    val ditNavTekst: String,
    val varseltype: VarselType,
    val epostTitel: String? = null, //Disse settes til standartekst av brukernotifiaksjoenr hvis ikke satt
    val epostBody: String? = null,
    val smsTekst: String? = null
) {
    constructor(aktivitetId: Long,
                aktitetVersion: Long,
                aktorId: Person.AktorId,
                ditNavTekst: String,
                varseltype: VarselType,): this(
                aktivitetId,
                aktitetVersion,
                aktorId,
                ditNavTekst,
                varseltype,
                    null, null, null
                )

    fun toUgåendeVarsel(
        brukernotifikasjonsId: UUID,
        gjeldendeOppfolgingsperiode: UUID,
        aktivitetBasePath: String,
        fnr: Person.Fnr
    ): UtgåendeVarsel {
        return UtgåendeVarsel(
            MinSideBrukernotifikasjonsId(brukernotifikasjonsId),
            fnr,
            ditNavTekst,
            gjeldendeOppfolgingsperiode,
            varseltype,
            VarselStatus.PENDING,
            createAktivitetLink(aktivitetBasePath, aktivitetId.toString()),
            null,
            null,
            null)
    }
}
