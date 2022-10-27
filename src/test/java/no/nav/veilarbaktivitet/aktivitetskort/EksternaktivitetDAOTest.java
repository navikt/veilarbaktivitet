package no.nav.veilarbaktivitet.aktivitetskort;


import lombok.SneakyThrows;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.config.database.Database;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class EksternaktivitetDAOTest {

    @Test
    @SneakyThrows
    public void test_json_serialisering() {
        JdbcTemplate jdbcTemplate = LocalH2Database.getPresistentDb();
        Database database = new Database(jdbcTemplate);
        AktivitetDAO aktivitetDAO = new AktivitetDAO(database, database.getNamedJdbcTemplate());

        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEksternAktivitet();
        AktivitetData opprettetAktivitetData = aktivitetDAO.opprettNyAktivitet(aktivitetData);

        AktivitetData aktivitet = aktivitetDAO.hentAktivitet(opprettetAktivitetData.getId());
        Assertions.assertThat(aktivitet).isEqualTo(opprettetAktivitetData);
    }

}
