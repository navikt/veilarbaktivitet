package no.nav.veilarbaktivitet.util;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class DateUtilsTest {
    @Test
    void dateFromISO8601_should_return_same_date_as_zuluDateTimeFromDate() {
        String original = "2010-12-03T10:15:30+02:00";
        Date dateFromString = DateUtils.dateFromISO8601(original);
        String fromDate = DateUtils.zuluDateTimeFromDate(dateFromString);

        assertThat(fromDate).isEqualTo(original);
    }
}