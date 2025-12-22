package no.nav.veilarbaktivitet.arkivering

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.mappers.Helpers
import no.nav.veilarbaktivitet.util.DateUtils
import java.time.ZonedDateTime

fun filtrerArkiveringsData(
    arkiveringsData: ArkiveringsData,
    filter: ArkiveringsController.Filter
): ArkiveringsData {
    return arkiveringsData
        .filtrerPåHistorikk(filter)
        .filtrerPåAvtaltMedNavn(filter)
        .filtrerPåStillingsstatus(filter)
        .filtrerPåArenaAktivitetStatus(filter)
        .filtrerPaAktivitetType(filter)
        .filtrerInkluderDialoger(filter)
        .filtrerPåKvpPeriode(filter)
}

private fun ArkiveringsData.filtrerPåKvpPeriode(filter: ArkiveringsController.Filter): ArkiveringsData {
    return when (filter.kvpUtvalgskriterie.alternativ) {
        ArkiveringsController.KvpUtvalgskriterieAlternativ.EKSKLUDER_KVP_AKTIVITETER -> {
            this.copy(
                aktiviteter = this.aktiviteter.filter { it.kontorsperreEnhetId == null },
                dialoger = this.dialoger.filter { it.kontorsperreEnhetId == null }
            )
        }
        ArkiveringsController.KvpUtvalgskriterieAlternativ.INKLUDER_KVP_AKTIVITETER -> {
            this
        }
        ArkiveringsController.KvpUtvalgskriterieAlternativ.KUN_KVP_AKTIVITETER -> {
            val kvpStart = filter.kvpUtvalgskriterie.start
            val kvpSlutt = filter.kvpUtvalgskriterie.slutt
            val aktiviteterIKvpPerioden = this.aktiviteter.filter { aktivitet ->
                val opprettetDato = DateUtils.dateToZonedDateTime(aktivitet.opprettetDato)
                aktivitet.kontorsperreEnhetId != null && opprettetDato.iTidsrom(kvpStart, kvpSlutt)
            }
            val dialogerIKvpPerioden = this.dialoger.filter { dialog ->
                dialog.kontorsperreEnhetId != null && dialog.opprettetDato.iTidsrom(kvpStart, kvpSlutt)
            }
            this.copy(aktiviteter = aktiviteterIKvpPerioden, dialoger = dialogerIKvpPerioden)
        }
    }
}

private fun ArkiveringsData.filtrerPåHistorikk(filter: ArkiveringsController.Filter): ArkiveringsData {
    return if (!filter.inkluderHistorikk) {
        this.copy(historikkForAktiviteter = emptyMap())
    } else {
        this
    }
}

private fun ArkiveringsData.filtrerPåAvtaltMedNavn(filter: ArkiveringsController.Filter): ArkiveringsData {
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

private fun ArkiveringsData.filtrerPåStillingsstatus(filter: ArkiveringsController.Filter): ArkiveringsData {
    if (filter.stillingsstatusFilter.isEmpty()) return this
    val stillingsstatusFilterSomTekst = filter.stillingsstatusFilter.map { it.name }

    val filtrerteAktiviteter = aktiviteter.filter { aktivitet ->
        if (aktivitet.stillingFraNavData == null && aktivitet.stillingsSoekAktivitetData == null) true
        else if (aktivitet.stillingFraNavData != null) aktivitet.stillingFraNavData.soknadsstatus.name in stillingsstatusFilterSomTekst
        else aktivitet.stillingsSoekAktivitetData.stillingsoekEtikett?.name in stillingsstatusFilterSomTekst
    }
    return this.copy(aktiviteter = filtrerteAktiviteter)
}

private fun ArkiveringsData.filtrerPåArenaAktivitetStatus(filter: ArkiveringsController.Filter): ArkiveringsData {
    if (filter.arenaAktivitetStatusFilter.isEmpty()) return this

    val oppdaterteArenaAktiviteter = arenaAktiviteter.filter { aktivitet ->
        aktivitet.etikett in filter.arenaAktivitetStatusFilter
    }
    return this.copy(arenaAktiviteter = oppdaterteArenaAktiviteter)
}

private fun ArkiveringsData.filtrerPaAktivitetType(filter: ArkiveringsController.Filter): ArkiveringsData {
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

private fun ArkiveringsData.filtrerInkluderDialoger(filter: ArkiveringsController.Filter): ArkiveringsData {
    if (filter.inkluderDialoger) return this
    return this.copy(dialoger = emptyList())
}

private fun ZonedDateTime.iTidsrom(fra: ZonedDateTime?, til: ZonedDateTime?) =
    this.isAfter(fra) && this.isBefore(til)