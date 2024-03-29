package no.nav.veilarbaktivitet.aktivitetskort.bestilling

import no.nav.veilarbaktivitet.aktivitetskort.ActionType
import no.nav.veilarbaktivitet.aktivitetskort.dto.Aktivitetskort
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType
import no.nav.veilarbaktivitet.arena.model.ArenaId
import no.nav.veilarbaktivitet.person.Person
import java.time.ZonedDateTime
import java.util.*

class ArenaAktivitetskortBestilling(
    aktivitetskort: Aktivitetskort,
    source: String,
    type: AktivitetskortType,
    val eksternReferanseId: ArenaId,
    val arenaTiltakskode: String,
    val oppfolgingsperiode: UUID,
    val oppfolgingsperiodeSlutt: ZonedDateTime?,
    messageId: UUID,
    actionType: ActionType,
    aktorId: Person.AktorId,
) : AktivitetskortBestilling(source, type, aktivitetskort, messageId, actionType, aktorId) {

    override fun getAktivitetskortId() = aktivitetskort.id
}
