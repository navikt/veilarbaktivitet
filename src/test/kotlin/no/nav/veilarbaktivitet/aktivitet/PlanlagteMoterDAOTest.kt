package no.nav.veilarbaktivitet.aktivitet

import no.nav.common.types.identer.AktorId
import no.nav.veilarbaktivitet.LocalDatabaseSingleton
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.domain.PlanlagtMote
import no.nav.veilarbaktivitet.person.Person
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.Instant
import java.util.Date
import java.util.concurrent.ThreadLocalRandom

class PlanlagteMoterDAOTest {
    private val jdbc = NamedParameterJdbcTemplate(LocalDatabaseSingleton.postgres)
    private val dao = AktivitetDAO(jdbc)
    private val planlagteMoterDAO = PlanlagteMoterDAO(jdbc)
    private val aktorId = nyAktorId()
    private val tidspunkt = Date.from(Instant.parse("2026-01-01T12:00:00Z"))

    fun mote(fraDatoIMinutterFraNå: Int?,
             historiskTidspunkt: Date? = null,
             status: AktivitetStatus = AktivitetStatus.PLANLAGT,
             aktorId: Person.AktorId = this.aktorId): AktivitetData {
        return AktivitetDataTestBuilder.nyMoteAktivitet()
            .withAktorId(aktorId)
            .withStatus(status)
            .withHistoriskDato(historiskTidspunkt)
            .withFraDato(fraDatoIMinutterFraNå?.let { Date(tidspunkt.time + it * 60_000) })
    }

    @ParameterizedTest
    @EnumSource(AktivitetStatus::class)
    fun `bare moter i en ikke-ferdig status teller som planlagt`(status: AktivitetStatus) {
        val mote = dao.opprettNyAktivitet(
            AktivitetDataTestBuilder.nyMoteAktivitet()
                .withAktorId(aktorId)
                .withFraDato(Date(tidspunkt.time + 60_000))
                .withStatus(status)
        )

        val resultat = planlagteMoterDAO.hentPlanlagteMoter(aktorId, tidspunkt)

        if (status == AktivitetStatus.FULLFORT || status == AktivitetStatus.AVBRUTT) {
            assertThat(resultat).isEmpty()
        } else {
            assertThat(resultat).containsExactly(PlanlagtMote(mote.id, mote.fraDato))
        }
    }

    @Test
    fun `henter bare gjeldende fremtidige uferdige moter for riktig bruker`() {
        dao.opprettNyAktivitet(mote(-1))
        dao.opprettNyAktivitet(mote(0))
        dao.opprettNyAktivitet(mote(null))
        dao.opprettNyAktivitet(mote(1, historiskTidspunkt = tidspunkt))
        dao.opprettNyAktivitet(mote(1, status = AktivitetStatus.FULLFORT))
        dao.opprettNyAktivitet(mote(1, status = AktivitetStatus.AVBRUTT))
        dao.opprettNyAktivitet(mote(1, aktorId = nyAktorId()))
        dao.opprettNyAktivitet(
            AktivitetDataTestBuilder.nySamtaleReferat().withAktorId(aktorId).withFraDato(mote(1).fraDato)
        )
        val avbrutt = dao.opprettNyAktivitet(mote(1))
        dao.oppdaterAktivitet(avbrutt.withStatus(AktivitetStatus.AVBRUTT))
        val flyttet = dao.opprettNyAktivitet(mote(1))
        val flyttetMote = dao.oppdaterAktivitet(flyttet.withFraDato(mote(10).fraDato))
        val senere = dao.opprettNyAktivitet(mote(5))
        val neste = dao.opprettNyAktivitet(mote(2).withStatus(AktivitetStatus.GJENNOMFORES))

        assertThat(planlagteMoterDAO.hentPlanlagteMoter(aktorId, tidspunkt))
            .containsExactly(
                PlanlagtMote(neste.id, neste.fraDato),
                PlanlagtMote(senere.id, senere.fraDato),
                PlanlagtMote(flyttetMote.id, flyttetMote.fraDato),
            )
        assertThat(planlagteMoterDAO.hentPlanlagteMoter(nyAktorId(), tidspunkt))
            .isEmpty()
    }

    @Test
    fun `henter alle planlagte moter uavhengig av kontorsperre`() {
        val skjult = dao.opprettNyAktivitet(
            AktivitetDataTestBuilder.nyMoteAktivitet()
                .withAktorId(aktorId)
                .withFraDato(Date(tidspunkt.time + 60_000))
                .withKontorsperreEnhetId("1234")
        )
        val ogsaSkjult = dao.opprettNyAktivitet(
            AktivitetDataTestBuilder.nyMoteAktivitet()
                .withAktorId(aktorId)
                .withFraDato(Date(tidspunkt.time + 90_000))
                .withKontorsperreEnhetId("1234")
        )
        val synlig = dao.opprettNyAktivitet(
            AktivitetDataTestBuilder.nyMoteAktivitet()
                .withAktorId(aktorId)
                .withFraDato(Date(tidspunkt.time + 120_000))
        )

        assertThat(planlagteMoterDAO.hentPlanlagteMoter(aktorId, tidspunkt))
            .containsExactly(
                PlanlagtMote(skjult.id, skjult.fraDato, "1234"),
                PlanlagtMote(ogsaSkjult.id, ogsaSkjult.fraDato, "1234"),
                PlanlagtMote(synlig.id, synlig.fraDato),
            )
    }

    @Test
    fun `sorterer pa id nar flere moter har samme starttid`() {
        val mote = AktivitetDataTestBuilder.nyMoteAktivitet()
            .withAktorId(aktorId)
            .withFraDato(Date(tidspunkt.time + 60_000))
        val forste = dao.opprettNyAktivitet(mote)
        val andre = dao.opprettNyAktivitet(mote)

        assertThat(planlagteMoterDAO.hentPlanlagteMoter(aktorId, tidspunkt))
            .containsExactly(
                PlanlagtMote(forste.id, forste.fraDato),
                PlanlagtMote(andre.id, andre.fraDato),
            )
    }

    private fun nyAktorId() = Person.aktorId(
        ThreadLocalRandom.current().nextLong(1_000_000_000_000, 10_000_000_000_000).toString()
    )
}
