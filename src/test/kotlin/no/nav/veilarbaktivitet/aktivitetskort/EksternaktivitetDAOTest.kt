package no.nav.veilarbaktivitet.aktivitetskort

import lombok.SneakyThrows
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO
import no.nav.veilarbaktivitet.config.database.Database
import no.nav.veilarbaktivitet.mock.LocalH2Database
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


internal class EksternaktivitetDAOTest {
    @Test
    @SneakyThrows
    fun test_json_serialisering_av_eksternaktivitet() {
        val jdbcTemplate = LocalH2Database.getPresistentDb()
        val database = Database(jdbcTemplate)
        val aktivitetDAO = AktivitetDAO(database.namedJdbcTemplate)

        val aktivitetData = AktivitetDataTestBuilder.nyEksternAktivitet()
        val opprettetAktivitetData = aktivitetDAO.opprettNyAktivitet(aktivitetData)
        val aktivitet = aktivitetDAO.hentAktivitet(opprettetAktivitetData.id)
        val utEkstern = aktivitet.eksternAktivitetData
        val innEkstern = opprettetAktivitetData.eksternAktivitetData

        assertThat(utEkstern.endretTidspunktKilde).isEqualToIgnoringNanos(innEkstern.endretTidspunktKilde)
        assertThat(utEkstern).isEqualTo(innEkstern.copy(endretTidspunktKilde = utEkstern.endretTidspunktKilde))
        assertThat(aktivitet.withEksternAktivitetData(null)).isEqualTo(opprettetAktivitetData.withEksternAktivitetData(null))
    }
}
