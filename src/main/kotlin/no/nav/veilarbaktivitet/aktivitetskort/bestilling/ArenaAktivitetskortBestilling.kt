package no.nav.veilarbaktivitet.aktivitetskort.bestilling

import no.nav.veilarbaktivitet.aktivitetskort.ActionType
import no.nav.veilarbaktivitet.aktivitetskort.Aktivitetskort
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortType
import no.nav.veilarbaktivitet.arena.model.ArenaId
import no.nav.veilarbaktivitet.person.Person
import java.util.*

class ArenaAktivitetskortBestilling(
    aktivitetskort: Aktivitetskort,
    source: String,
    type: AktivitetskortType,
    val eksternReferanseId: ArenaId,
    val arenaTiltakskode: String,
    messageId: UUID,
    actionType: ActionType,
    aktorId: Person.AktorId,
) : AktivitetskortBestilling(source, type, aktivitetskort, messageId, actionType, aktorId) {

    override fun getAktivitetskortId() = aktivitetskort.id
}
