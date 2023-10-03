package no.nav.veilarbaktivitet.aktivitetskort.bestilling

import no.nav.common.types.identer.NavIdent
import no.nav.common.types.identer.NorskIdent
import no.nav.veilarbaktivitet.aktivitetskort.ActionType
import no.nav.veilarbaktivitet.aktivitetskort.dto.BestillingBase
import java.util.*

class KasseringsBestilling(
    source: String,
    messageId: UUID,
    val navIdent: NavIdent,
    val personIdent: NorskIdent,
    val aktivitetsId: UUID,
    val begrunnelse: String?
) : BestillingBase(source, messageId, ActionType.KASSER_AKTIVITET) {
    override fun getAktivitetskortId() = aktivitetsId
}
