package no.nav.veilarbaktivitet.oppfolging.periode

import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.chrono.ChronoZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs

val log = LoggerFactory.getLogger("no.nav.veilarbaktivitet.oppfolging.periode.OppfolgingsperiodeUtil")!!


fun finnOppfolgingsperiodeForArenaAktivitet(oppfolgingsperioder: List<Oppfolgingsperiode>, aktivitetOppslagsdato: LocalDate?): Oppfolgingsperiode? {
    return aktivitetOppslagsdato?.let { oppfolgingsperioder.finnOppfolgingsperiodeForTidspunkt(it.atStartOfDay()) }
}

fun List<Oppfolgingsperiode>.finnOppfolgingsperiodeForTidspunkt(aktivitetOppslagsdato: LocalDateTime): Oppfolgingsperiode? {
    val oppfolgingsperioder = this.sortedByDescending { it.startTid }
    if (oppfolgingsperioder.isEmpty()) {
        log.info("Arenatiltak finn oppfølgingsperiode - bruker har ingen oppfølgingsperioder - tidspunkt=${aktivitetOppslagsdato}, oppfolgingsperioder=${listOf<OppfolgingPeriodeMinimalDTO>()}")
        return null
    }

    val opprettetTidspunktCDT = aktivitetOppslagsdato.atZone(ZoneId.systemDefault())

    val match = oppfolgingsperioder
        .firstOrNull { oppfolgingsperiode -> oppfolgingsperiode.erInnenforPeriode(opprettetTidspunktCDT) }
        ?: oppfolgingsperioder
            .filterIndexed { index, oppfolgingsperiode ->
                val forrigePeriode = oppfolgingsperioder.getOrNull(index + 1)
                val aktivitetStartetEtterAtForrigePeriodeBleAvsluttet = forrigePeriode?.sluttTid?.let { opprettetTidspunktCDT.isAfter(it) } ?: false
                val aktivitetStartetRettFørDennePeriodenBleStartet = oppfolgingsperiode.erInnenforMedEkstraSlack(opprettetTidspunktCDT)
                aktivitetStartetEtterAtForrigePeriodeBleAvsluttet || aktivitetStartetRettFørDennePeriodenBleStartet
            }
            .minByOrNull { abs(ChronoUnit.MILLIS.between(opprettetTidspunktCDT, it.startTid)) }
            ?.also { _ ->
                log.info("Arenatiltak finn oppfølgingsperiode - valgt oppfølgingsperiode som startet etter opprettetdato) - tidspunkt=${aktivitetOppslagsdato}, oppfolgingsperioder=${oppfolgingsperioder}")
            }

    return if (match != null) {
        match
    } else {
        log.info("Arenatiltak finn oppfølgingsperiode - opprettetTidspunkt har ingen god match på oppfølgingsperioder) - tidspunkt=${aktivitetOppslagsdato}, oppfolgingsperioder=${oppfolgingsperioder}")
        null
    }
}

fun Oppfolgingsperiode.erInnenforPeriode(opprettetTidspunktCZDT: ZonedDateTime): Boolean {
    return this.startTid.isBeforeOrEqual(opprettetTidspunktCZDT) &&
            (this.sluttTid?.isAfter(opprettetTidspunktCZDT) ?: true)
}

fun Oppfolgingsperiode.erInnenforMedEkstraSlack(opprettetTidspunktCZDT: ZonedDateTime): Boolean {
    val utvidetOppfolgingsperiode = Oppfolgingsperiode(
        this.aktorid,
        this.oppfolgingsperiodeId,
        this.startTid.minus(OppfolgingsperiodeService.SLACK_FOER),
        this.sluttTid
    )
    return utvidetOppfolgingsperiode.erInnenforPeriode(opprettetTidspunktCZDT)
}

private fun ZonedDateTime.isBeforeOrEqual(other: ChronoZonedDateTime<*>): Boolean {
    return this.isBefore(other) || this.isEqual(other)
}