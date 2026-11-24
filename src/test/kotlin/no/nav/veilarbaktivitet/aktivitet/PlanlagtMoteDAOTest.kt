package no.nav.veilarbaktivitet.aktivitet

import no.nav.veilarbaktivitet.LocalDatabaseSingleton
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.domain.PlanlagtMote
import no.nav.veilarbaktivitet.person.Person
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.Instant
import java.util.Date
import java.util.concurrent.ThreadLocalRandom

class PlanlagtMoteDAOTest {
    private val dao = AktivitetDAO(NamedParameterJdbcTemplate(LocalDatabaseSingleton.postgres))
    private val aktorId = nyAktorId()
    private val tidspunkt = Date.from(Instant.parse("2026-01-01T12:00:00Z"))

    @Test
    fun `henter bare gjeldende fremtidige uferdige moter for riktig bruker`() {
        fun mote(minutter: Long) = AktivitetDataTestBuilder.nyMoteAktivitet()
            .withAktorId(aktorId)
            .withFraDato(Date(tidspunkt.time + minutter * 60_000))

        dao.opprettNyAktivitet(mote(-1))
        dao.opprettNyAktivitet(mote(0))
        dao.opprettNyAktivitet(mote(1).withFraDato(null))
        dao.opprettNyAktivitet(mote(1).withHistoriskDato(tidspunkt))
        dao.opprettNyAktivitet(mote(1).withStatus(AktivitetStatus.FULLFORT))
        dao.opprettNyAktivitet(mote(1).withStatus(AktivitetStatus.AVBRUTT))
        dao.opprettNyAktivitet(mote(1).withAktorId(nyAktorId()))
        dao.opprettNyAktivitet(
            AktivitetDataTestBuilder.nySamtaleReferat().withAktorId(aktorId).withFraDato(mote(1).fraDato)
        )
        val avbrutt = dao.opprettNyAktivitet(mote(1))
        dao.oppdaterAktivitet(avbrutt.withStatus(AktivitetStatus.AVBRUTT))
        val flyttet = dao.opprettNyAktivitet(mote(1))
        dao.oppdaterAktivitet(flyttet.withFraDato(mote(10).fraDato))
        dao.opprettNyAktivitet(mote(5))
        val neste = dao.opprettNyAktivitet(mote(2).withStatus(AktivitetStatus.GJENNOMFORES))

        assertThat(dao.hentNestePlanlagteMote(aktorId, tidspunkt))
            .contains(PlanlagtMote(neste.id, neste.fraDato))
        assertThat(dao.hentNestePlanlagteMote(nyAktorId(), tidspunkt))
            .isEmpty()
    }

    @Test
    fun `velger neste tilgjengelige mote nar tidligste er kontorsperret`() {
        val skjult = dao.opprettNyAktivitet(
            AktivitetDataTestBuilder.nyMoteAktivitet()
                .withAktorId(aktorId)
                .withFraDato(Date(tidspunkt.time + 60_000))
                .withKontorsperreEnhetId("1234")
        )
        val synlig = dao.opprettNyAktivitet(
            AktivitetDataTestBuilder.nyMoteAktivitet()
                .withAktorId(aktorId)
                .withFraDato(Date(tidspunkt.time + 120_000))
        )

        assertThat(dao.hentNestePlanlagteMote(aktorId, tidspunkt))
            .contains(PlanlagtMote(skjult.id, skjult.fraDato))
        assertThat(dao.hentNestePlanlagteMote(aktorId, tidspunkt) { it.kontorsperreEnhetId == null })
            .contains(PlanlagtMote(synlig.id, synlig.fraDato))
    }

    private fun nyAktorId() = Person.aktorId(
        ThreadLocalRandom.current().nextLong(1_000_000_000_000, 10_000_000_000_000).toString()
    )
}
