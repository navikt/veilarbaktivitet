package no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel

import no.nav.veilarbaktivitet.arena.model.ArenaId
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselStatus
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType
import no.nav.veilarbaktivitet.person.Person
import java.util.*

data class ArenaAktivitetVarsel(
    val arenaAktivitetId: ArenaId,
    val aktivitetId: Optional<Long>,
    val fnr: Person.Fnr,
    val ditNavTekst: String,
) {
    fun toUgåendeVarsel(
        uuid: MinSideVarselId,
        gjeldendeOppfolgingsperiode: UUID,
        aktivitetBasePath: String,
    ): UtgåendeVarsel {
        return UtgåendeVarsel(
            uuid,
            fnr,
            ditNavTekst,
            gjeldendeOppfolgingsperiode,
            VarselType.FORHAANDSORENTERING,
            VarselStatus.PENDING,
            createAktivitetLink(aktivitetBasePath, aktivitetId.map { obj: Long -> obj.toString() }.orElseGet { arenaAktivitetId.id() }),
            null,
            null,
            null)
    }
}