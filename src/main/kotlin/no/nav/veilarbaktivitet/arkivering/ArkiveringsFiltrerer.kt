package no.nav.veilarbaktivitet.arkivering

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.mappers.Helpers
import no.nav.veilarbaktivitet.util.DateUtils

fun filtrerArkiveringsData(
    arkiveringsData: ArkiveringsController.ArkiveringsData,
    filter: ArkiveringsController.Filter
): ArkiveringsController.ArkiveringsData {
    return arkiveringsData
        .filtrerPåHistorikk(filter)
        .filtrerPåAvtaltMedNavn(filter)
        .filtrerPåStillingsstatus(filter)
        .filtrerPåArenaAktivitetStatus(filter)
        .filtrerPaAktivitetType(filter)
        .filtrerDialoger(filter)
        .filtrerPåKvpPeriode(filter)
}

private fun ArkiveringsController.ArkiveringsData.filtrerPåKvpPeriode(filter: ArkiveringsController.Filter): ArkiveringsController.ArkiveringsData {
    val ekskluderKvpAktiviteter = filter.kvpFilter == null
    if (ekskluderKvpAktiviteter) {
        val aktiviteterUtenKvp = this.aktiviteter.filter { it.kontorsperreEnhetId == null }
        return this.copy(aktiviteter = aktiviteterUtenKvp)
    }

    val inkluderKunKvpAktiviteterSomErIEtGittTidsrom = filter.kvpFilter != null && filter.kvpFilter.inkluderKunKvpAktiviteter
    if (inkluderKunKvpAktiviteterSomErIEtGittTidsrom) {
        val kunKvpAktiviteterITidsrom = this.aktiviteter.filter { aktivitet ->
            val opprettetDato = DateUtils.dateToZonedDateTime(aktivitet.opprettetDato)
            val erKvpAktivitetITidsrom = aktivitet.kontorsperreEnhetId != null &&
                    opprettetDato.isAfter(filter.kvpFilter.start) &&
                    opprettetDato.isBefore(filter.kvpFilter.slutt)

            erKvpAktivitetITidsrom
        }
        return this.copy(aktiviteter = kunKvpAktiviteterITidsrom)
    }

    val alleAktiviteterInkludertKvp = this.aktiviteter
    return this.copy(aktiviteter = alleAktiviteterInkludertKvp)
}

private fun ArkiveringsController.ArkiveringsData.filtrerPåHistorikk(filter: ArkiveringsController.Filter): ArkiveringsController.ArkiveringsData {
    return if (!filter.inkluderHistorikk) {
        this.copy(historikkForAktiviteter = emptyMap())
    } else {
        this
    }
}

private fun ArkiveringsController.ArkiveringsData.filtrerPåAvtaltMedNavn(filter: ArkiveringsController.Filter): ArkiveringsController.ArkiveringsData {
    if (filter.aktivitetAvtaltMedNavFilter.isEmpty()) return this

    val predikater = filter.aktivitetAvtaltMedNavFilter.map { filter ->
        when (filter) {
            ArkiveringsController.AvtaltMedNavFilter.AVTALT_MED_NAV -> { aktivitetData: AktivitetData -> aktivitetData.isAvtalt }
            ArkiveringsController.AvtaltMedNavFilter.IKKE_AVTALT_MED_NAV -> { aktivitetData: AktivitetData -> !aktivitetData.isAvtalt }
        }
    }
    val filtrerteAktiviteter = this.aktiviteter.filter { aktivitetData ->
        predikater.any { it(aktivitetData) }
    }
    return this.copy(aktiviteter = filtrerteAktiviteter)
}

private fun ArkiveringsController.ArkiveringsData.filtrerPåStillingsstatus(filter: ArkiveringsController.Filter): ArkiveringsController.ArkiveringsData {
    if (filter.stillingsstatusFilter.isEmpty()) return this
    val stillingsstatusFilterSomTekst = filter.stillingsstatusFilter.map { it.name }

    val filtrerteAktiviteter = aktiviteter.filter { aktivitet ->
        if (aktivitet.stillingFraNavData == null && aktivitet.stillingsSoekAktivitetData == null) true
        else if (aktivitet.stillingFraNavData != null) aktivitet.stillingFraNavData.soknadsstatus.name in stillingsstatusFilterSomTekst
        else aktivitet.stillingsSoekAktivitetData.stillingsoekEtikett.name in stillingsstatusFilterSomTekst
    }
    return this.copy(aktiviteter = filtrerteAktiviteter)
}

private fun ArkiveringsController.ArkiveringsData.filtrerPåArenaAktivitetStatus(filter: ArkiveringsController.Filter): ArkiveringsController.ArkiveringsData {
    if (filter.arenaAktivitetStatusFilter.isEmpty()) return this

    val oppdaterteArenaAktiviteter = arenaAktiviteter.filter { aktivitet ->
        aktivitet.etikett in filter.arenaAktivitetStatusFilter
    }
    return this.copy(arenaAktiviteter = oppdaterteArenaAktiviteter)
}

private fun ArkiveringsController.ArkiveringsData.filtrerPaAktivitetType(filter: ArkiveringsController.Filter): ArkiveringsController.ArkiveringsData {
    if (filter.aktivitetTypeFilter.isEmpty()) return this

    val aktivitetTypeFilterSomTekst = filter.aktivitetTypeFilter.map { it.name }

    // Mapper om aktivitetType til aktivitetTypeDTO pga. STILLING = JOBBSOKING
    val filtrerteAktiviteter = aktiviteter.filter { aktivitet ->
        Helpers.Type.getDTO(aktivitet.aktivitetType).name in aktivitetTypeFilterSomTekst
                || aktivitet.eksternAktivitetData?.type?.name in aktivitetTypeFilterSomTekst
    }
    val filtrerteArenaAktiviteter =
        if (filter.aktivitetTypeFilter.contains(ArkiveringsController.AktivitetTypeFilter.ARENA_TILTAK)) this.arenaAktiviteter else emptyList()

    return this.copy(aktiviteter = filtrerteAktiviteter, arenaAktiviteter = filtrerteArenaAktiviteter)
}

private fun ArkiveringsController.ArkiveringsData.filtrerDialoger(filter: ArkiveringsController.Filter): ArkiveringsController.ArkiveringsData {
    if (filter.inkluderDialoger) return this
    return this.copy(dialoger = emptyList())
}