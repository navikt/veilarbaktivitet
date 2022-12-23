package no.nav.veilarbaktivitet.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.Date;
class DateUtilsTest {
    @Test
    void should_return_correct_date() {
        String original = "2010-12-03T10:15:30+02:00";
        Date dateFromString = DateUtils.dateFromISO8601(original);
        String fromDate = DateUtils.ISO8601FromDate(dateFromString, ZoneId.of("+02:00"));

        Assertions.assertThat(fromDate).isEqualTo(original);
    }

}