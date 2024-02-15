package no.nav.veilarbaktivitet.aktivitetskort

import lombok.SneakyThrows
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO
import no.nav.veilarbaktivitet.config.database.Database
import no.nav.veilarbaktivitet.mock.LocalH2Database
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder
import no.nav.veilarbaktivitet.util.DateUtils
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test


internal class EksternaktivitetDAOTest {
    @Test
    @SneakyThrows
    fun test_json_serialisering() {
        val jdbcTemplate = LocalH2Database.getPresistentDb()
        val database = Database(jdbcTemplate)
        val aktivitetDAO = AktivitetDAO(database.namedJdbcTemplate)

        val aktivitetData = AktivitetDataTestBuilder.nyEksternAktivitet()
        val opprettetAktivitetData = aktivitetDAO.opprettNyAktivitet(aktivitetData)
        val aktivitet = aktivitetDAO.hentAktivitet(opprettetAktivitetData.id)
        val endretTidspunktKildeWithOraclePrescision =
            DateUtils.dateToZonedDateTime(DateUtils.zonedDateTimeToDate(opprettetAktivitetData.eksternAktivitetData.endretTidspunktKilde))
        Assertions.assertThat(aktivitet.eksternAktivitetData)
            .isEqualTo(opprettetAktivitetData.eksternAktivitetData.copy(endretTidspunktKilde = endretTidspunktKildeWithOraclePrescision))
        Assertions.assertThat(aktivitet.withEksternAktivitetData(null))
            .isEqualTo(opprettetAktivitetData.withEksternAktivitetData(null))
    }
}
