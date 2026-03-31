package no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter

import no.nav.veilarbaktivitet.aktivitet.AktivitetService.mergeBehandlingAktivitetData
import no.nav.veilarbaktivitet.aktivitet.AktivitetService.mergeEgenAktivitetData
import no.nav.veilarbaktivitet.aktivitet.AktivitetService.mergeEksternAktivitet
import no.nav.veilarbaktivitet.aktivitet.AktivitetService.mergeIJobbAktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import java.util.concurrent.ExecutionException

sealed interface AktivitetsEndring {
    val muterbareFelter: AktivitetMuterbareFelter
    val id: Long
    val versjon: Long
    val sporing: SporingsData

//    open fun berikMedAktivitetsTypeSpesifikkData(): AktivitetData
}

object AktivitetsEndringUtil {

    @JvmStatic
    fun berikMedAktivitetsTypeSpesifikkData(input: AktivitetData, endring: AktivitetsEndring): AktivitetData.AktivitetDataBuilder {
        .behandlingAktivitetData(merger.map(AktivitetData::getBehandlingAktivitetData).merge(this::mergeBehandlingAktivitetData))
            .egenAktivitetData(merger.map(AktivitetData::getEgenAktivitetData).merge(this::mergeEgenAktivitetData))
            .iJobbAktivitetData(merger.map(AktivitetData::getIJobbAktivitetData).merge(this::mergeIJobbAktivitetData))
            .moteData(merger.map(AktivitetData::getMoteData).merge(this::mergeMoteData))
            .sokeAvtaleAktivitetData(merger.map(AktivitetData::getSokeAvtaleAktivitetData).merge(this::mergeSokeAvtaleAktivitetData))
            .stillingFraNavData(merger.map(AktivitetData::getStillingFraNavData).merge(this::mergeStillingFraNav))
            .stillingsSoekAktivitetData(merger.map(AktivitetData::getStillingsSoekAktivitetData).merge(this::mergeStillingSok))
            .eksternAktivitetData(merger.map(AktivitetData::getEksternAktivitetData).merge(this::mergeEksternAktivitet));

        when (endring) {
            is Behandling.Endre -> mergeBehandlingAktivitetData(input.behandlingAktivitetData, endring.behandlingAktivitetData)
            is Egenaktivitet.Endre -> mergeEgenAktivitetData(input.egenAktivitetData, endring.egenAktivitetData)
            is Eksternaktivitet.Endre -> mergeEksternAktivitet(input.eksternAktivitetData, endring.eksternAktivitetData)
            is Ijobb.Endre -> mergeIJobbAktivitetData(input.iJobbAktivitetData, endring.iJobbAktivitetData)
            is Jobbsoeking.Endre -> throw Exception("asd")
            // OBS!
            is Mote.Endre,
            is Samtalereferat.Endre -> TODO()
            is Sokeavtale.Endre -> TODO()
            is StillingFraNav.Endre -> TODO()
        }
        return input
    }
}