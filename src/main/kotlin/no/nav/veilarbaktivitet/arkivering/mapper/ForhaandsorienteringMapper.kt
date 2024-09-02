package no.nav.veilarbaktivitet.arkivering.mapper

import no.nav.veilarbaktivitet.arkivering.ArkivFHO
import no.nav.veilarbaktivitet.avtalt_med_nav.Forhaandsorientering
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDTO
import no.nav.veilarbaktivitet.util.DateUtils.norskDatoOgKlokkeslett



fun Forhaandsorientering.toArkivForhaandsorientering() =
    this.toDTO().toArkivForhaandsorientering()

fun ForhaandsorienteringDTO.toArkivForhaandsorientering(): ArkivFHO? {
    if (this.tekst == null) return null
    return ArkivFHO(this.tekst, this.lestDato?.let { tidspunkt -> norskDatoOgKlokkeslett(tidspunkt) })
}
