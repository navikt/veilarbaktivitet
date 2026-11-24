@file:JvmName("PlanlagteMoter")

package no.nav.veilarbaktivitet.aktivitet.domain

import java.util.Date
import java.util.function.Predicate

data class PlanlagtMote(val id: Long, val fraDato: Date)

fun AktivitetData.erFremtidigPlanlagtMote(tidspunkt: Date): Boolean =
    aktivitetType == AktivitetTypeData.MOTE &&
        status != AktivitetStatus.FULLFORT &&
        status != AktivitetStatus.AVBRUTT &&
        historiskDato == null &&
        fraDato?.after(tidspunkt) == true

fun Iterable<AktivitetData>.nestePlanlagteMote(
    tidspunkt: Date,
    aktivitetsfilter: Predicate<AktivitetData> = Predicate { true },
): PlanlagtMote? =
    asSequence()
        .filter { it.erFremtidigPlanlagtMote(tidspunkt) }
        .filter { aktivitetsfilter.test(it) }
        .minWithOrNull(compareBy<AktivitetData> { it.fraDato }.thenBy { it.id })
        ?.let { PlanlagtMote(it.id, it.fraDato) }
