package no.nav.veilarbaktivitet.util;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class DateUtilsTest {
    @Test
    void dateFromISO8601_should_return_same_date_as_iso8601Fromdate() {
        String original = "2010-12-03T10:15:30+02:00";
        Date dateFromString = DateUtils.dateFromISO8601(original);
        String fromDate = DateUtils.iso8601Fromdate(dateFromString, ZoneId.of("+02:00"));

        assertThat(fromDate).isEqualTo(original);
    }
}