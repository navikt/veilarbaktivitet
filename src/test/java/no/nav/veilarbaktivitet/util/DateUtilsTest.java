package no.nav.veilarbaktivitet.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

class DateUtilsTest {
    @Test
    void dateFromISO8601_should_return_same_date_as_iso8601Fromdate() {
        String original = "2010-12-03T10:15:30+02:00";
        Date dateFromString = DateUtils.dateFromISO8601(original);
        String fromDate = DateUtils.iso8601Fromdate(dateFromString, ZoneId.of("+02:00"));

        assertThat(fromDate).isEqualTo(original);
    }

    @Test
    void test() {
        var local = LocalDateTime.now();
        var zoned = ZonedDateTime.now();
        assertThat(local).isCloseTo(zoned.toLocalDateTime(), within(1, ChronoUnit.MILLIS));
    }

    @Test
    void test2() {
        var local = LocalDateTime.now();
        var zoned = ZonedDateTime.now().withZoneSameInstant(ZoneId.of("+02:00"));
        assertThat(local).isCloseTo(zoned.toLocalDateTime(), within(1, ChronoUnit.MILLIS));
    }

}