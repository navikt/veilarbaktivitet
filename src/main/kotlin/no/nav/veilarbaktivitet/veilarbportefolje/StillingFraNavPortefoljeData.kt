package no.nav.veilarbaktivitet.veilarbportefolje

import java.sql.Date
import java.util.*

data class StillingFraNavPortefoljeData(val cvKanDelesStatus: CvKanDelesStatus, val svarfrist: Date) {
    companion object {
        @JvmStatic
        fun hvisStillingFraNavDataFinnes(
            id: Any?,
            cvKanDelesBoolean: Boolean?,
            svarfrist: Date
        ): StillingFraNavPortefoljeData? {
            if (Objects.isNull(id)) return null

            val cvKanDeles = CvKanDelesStatus.valueOf(cvKanDelesBoolean)

            return StillingFraNavPortefoljeData(cvKanDeles, svarfrist)
        }
    }
}
