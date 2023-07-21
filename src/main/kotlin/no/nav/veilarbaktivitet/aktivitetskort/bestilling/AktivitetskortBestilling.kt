package no.nav.veilarbaktivitet.aktivitetskort.bestilling

import no.nav.veilarbaktivitet.aktivitetskort.ActionType
import no.nav.veilarbaktivitet.aktivitetskort.Aktivitetskort
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortType
import no.nav.veilarbaktivitet.aktivitetskort.dto.BestillingBase
import no.nav.veilarbaktivitet.person.Person
import java.util.*

abstract class AktivitetskortBestilling (
    source: String,
    val aktivitetskortType: AktivitetskortType,
    val aktivitetskort: Aktivitetskort,
    messageId: UUID,
    actionType: ActionType,
    val aktorId: Person.AktorId
) : BestillingBase(source, messageId, actionType)
