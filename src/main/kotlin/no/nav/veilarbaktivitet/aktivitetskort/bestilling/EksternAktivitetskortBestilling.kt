package no.nav.veilarbaktivitet.aktivitetskort.bestilling

import no.nav.veilarbaktivitet.aktivitetskort.ActionType
import no.nav.veilarbaktivitet.aktivitetskort.dto.Aktivitetskort
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType
import no.nav.veilarbaktivitet.person.Person
import java.util.*

class EksternAktivitetskortBestilling(
    aktivitetskort: Aktivitetskort,
    source: String,
    type: AktivitetskortType,
    messageId: UUID,
    actionType: ActionType,
    aktorId: Person.AktorId
) : AktivitetskortBestilling(source, type, aktivitetskort, messageId, actionType, aktorId) {
    override fun getAktivitetskortId() = aktivitetskort.id

}