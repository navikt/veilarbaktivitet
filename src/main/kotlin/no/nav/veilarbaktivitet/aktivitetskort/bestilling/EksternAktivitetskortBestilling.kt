package no.nav.veilarbaktivitet.aktivitetskort.bestilling

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitetskort.ActionType
import no.nav.veilarbaktivitet.aktivitetskort.Aktivitetskort
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortMapper.toAktivitetsData
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortType
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO
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

    override fun toAktivitet(oppfolgingsPeriode: OppfolgingPeriodeMinimalDTO?): AktivitetData {
        return this.toAktivitetsData(null, oppfolgingsPeriode)
    }
}