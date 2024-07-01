package no.nav.veilarbaktivitet.aktivitetskort

import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule
import lombok.SneakyThrows
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate


internal class EksternaktivitetDAOTest {

    @JvmField
    @Rule
    var postgres: SingleInstancePostgresRule = EmbeddedPostgresRules.singleInstance()

    var jdbc = NamedParameterJdbcTemplate(postgres.embeddedPostgres.postgresDatabase)

    @Test
    @SneakyThrows
    fun test_json_serialisering_av_eksternaktivitet() {
        val aktivitetDAO = AktivitetDAO(jdbc)

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
