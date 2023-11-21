package no.nav.veilarbaktivitet.oppfolging.periode

import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO
import no.nav.veilarbaktivitet.util.DateUtils
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.chrono.ChronoZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs

val log = LoggerFactory.getLogger("no.nav.veilarbaktivitet.oppfolging.periode.OppfolgingsperiodeUtil")!!

fun List<Oppfolgingsperiode>.finnOppfolgingsperiodeForArenaAktivitet(arenaAktivitetDTO: ArenaAktivitetDTO): Oppfolgingsperiode? {
    return this.finnOppfolgingsperiodeForTidspunkt(DateUtils.dateToLocalDateTime(arenaAktivitetDTO.statusSistEndret))
        ?: this.finnOppfolgingsperiodeForTidspunkt(DateUtils.dateToLocalDateTime(arenaAktivitetDTO.tilDato))
}

fun List<Oppfolgingsperiode>.finnOppfolgingsperiodeForTidspunkt(tidspunkt: LocalDateTime): Oppfolgingsperiode? {
    val oppfolgingsperioder = this
    if (oppfolgingsperioder.isEmpty()) {
        log.info("Arenatiltak finn oppfølgingsperiode - bruker har ingen oppfølgingsperioder - tidspunkt=${tidspunkt}, oppfolgingsperioder=${listOf<OppfolgingPeriodeMinimalDTO>()}")
        return null
    }

    val opprettetTidspunktCZDT = tidspunkt.atZone(ZoneId.systemDefault())
    val match = oppfolgingsperioder
        .firstOrNull { oppfolgingsperiode -> oppfolgingsperiode.erInnenforPeriode(opprettetTidspunktCZDT) }
        ?: oppfolgingsperioder
            .filter { oppfolgingsperiode -> oppfolgingsperiode.erInnenforMedEkstraSlack(opprettetTidspunktCZDT) }
            .minByOrNull { abs(ChronoUnit.MILLIS.between(opprettetTidspunktCZDT, it.startTid)) }
            ?.also { _ ->
                log.info("Arenatiltak finn oppfølgingsperiode - opprettetdato innen 1 uke før oppfølging startdato) - tidspunkt=${tidspunkt}, oppfolgingsperioder=${oppfolgingsperioder}")
            }

    return if (match != null) {
        match
    } else {
        log.info("Arenatiltak finn oppfølgingsperiode - opprettetTidspunkt har ingen god match på oppfølgingsperioder) - tidspunkt=${tidspunkt}, oppfolgingsperioder=${oppfolgingsperioder}")
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
        this.oppfolgingsperiode,
        this.startTid.minus(OppfolgingsperiodeService.SLACK_FOER),
        this.sluttTid
    )
    return utvidetOppfolgingsperiode.erInnenforPeriode(opprettetTidspunktCZDT)
}

private fun ZonedDateTime.isBeforeOrEqual(other: ChronoZonedDateTime<*>): Boolean {
    return this.isBefore(other) || this.isEqual(other)
}