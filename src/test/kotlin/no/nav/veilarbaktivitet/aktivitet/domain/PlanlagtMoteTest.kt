package no.nav.veilarbaktivitet.aktivitet.domain

import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.Instant
import java.util.Date
import java.util.function.Predicate

class PlanlagtMoteTest {
    private val tidspunkt = Date.from(Instant.parse("2026-01-01T12:00:00Z"))
    private val fremtid = Date(tidspunkt.time + 60_000)
    private val mote = AktivitetDataTestBuilder.nyMoteAktivitet().withFraDato(fremtid)

    @ParameterizedTest
    @EnumSource(AktivitetStatus::class)
    fun `alle uferdige statuser teller som planlagt`(status: AktivitetStatus) {
        assertThat(mote.withStatus(status).erFremtidigPlanlagtMote(tidspunkt))
            .isEqualTo(status != AktivitetStatus.FULLFORT && status != AktivitetStatus.AVBRUTT)
    }

    @Test
    fun `bare ikke-historiske moter med start strengt i fremtiden teller`() {
        val ikkePlanlagte = listOf(
            mote.withFraDato(null),
            mote.withFraDato(Date(tidspunkt.time - 1)),
            mote.withFraDato(tidspunkt),
            mote.withHistoriskDato(tidspunkt),
            mote.withAktivitetType(AktivitetTypeData.SAMTALEREFERAT),
            mote.withAktivitetType(AktivitetTypeData.EGENAKTIVITET),
        )

        assertThat(ikkePlanlagte.map { it.erFremtidigPlanlagtMote(tidspunkt) })
            .containsOnly(false)
        assertThat(mote.erFremtidigPlanlagtMote(tidspunkt)).isTrue()
    }

    @Test
    fun `velger tidligste synlige mote med deterministisk id ved samme starttid`() {
        val skjult = mote.withId(1L).withFraDato(Date(tidspunkt.time + 1))
            .withKontorsperreEnhetId("1234")
        val neste = mote.withId(2L)
        val sammeTid = mote.withId(3L)
        val senere = mote.withId(4L).withFraDato(Date(fremtid.time + 60_000))
        val fullfort = skjult.withKontorsperreEnhetId(null).withStatus(AktivitetStatus.FULLFORT)

        val result = listOf(senere, sammeTid, fullfort, skjult, neste).nestePlanlagteMote(
            tidspunkt,
            Predicate { it.kontorsperreEnhetId == null },
        )

        assertThat(result).isEqualTo(PlanlagtMote(2L, fremtid))
        assertThat(listOf(fullfort, skjult).nestePlanlagteMote(
            tidspunkt,
            Predicate { it.kontorsperreEnhetId == null },
        )).isNull()
        assertThat(emptyList<AktivitetData>().nestePlanlagteMote(tidspunkt, Predicate { true })).isNull()
    }
}
