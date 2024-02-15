package no.nav.veilarbaktivitet.aktivitetskort;


import lombok.SneakyThrows;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.config.database.Database;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import no.nav.veilarbaktivitet.util.DateUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;

class EksternaktivitetDAOTest {

    @Test
    @SneakyThrows
    void test_json_serialisering() {
        JdbcTemplate jdbcTemplate = LocalH2Database.getPresistentDb();
        Database database = new Database(jdbcTemplate);
        AktivitetDAO aktivitetDAO = new AktivitetDAO(database.getNamedJdbcTemplate());

        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEksternAktivitet();
        AktivitetData opprettetAktivitetData = aktivitetDAO.opprettNyAktivitet(aktivitetData);
        // 23.626 584 772 Z
        // 23.626 585 Z
        AktivitetData aktivitet = aktivitetDAO.hentAktivitet(opprettetAktivitetData.getId());
        ZonedDateTime endretTidspunktKildeWithOraclePrescision =
                DateUtils.dateToZonedDateTime(DateUtils.zonedDateTimeToDate(opprettetAktivitetData.getEksternAktivitetData().getEndretTidspunktKilde()));
        Assertions.assertThat(aktivitet.getEksternAktivitetData()).isEqualTo(opprettetAktivitetData.getEksternAktivitetData());
        Assertions.assertThat(aktivitet.withEksternAktivitetData(null)).isEqualTo(opprettetAktivitetData.withEksternAktivitetData(null));
    }

}
