package no.nav.veilarbaktivitet.aktivitet.domain

import no.nav.common.types.identer.EnhetId
import java.util.Date

data class PlanlagtMote(
    val id: Long,
    val fraDato: Date,
    val kontorsperreEnhetId: EnhetId? = null,
) {

    constructor(id: Long, fraDato: Date, kontorsperreEnhetId: String?):
            this(id, fraDato, kontorsperreEnhetId?.let { EnhetId(it) })

    companion object {
        @JvmField
        val IKKE_LENGER_PLANLAGT_STATUSER = listOf(AktivitetStatus.AVBRUTT, AktivitetStatus.FULLFORT)
    }
}
