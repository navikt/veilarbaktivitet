package no.nav.veilarbaktivitet.util;

import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static no.nav.veilarbaktivitet.util.DateUtils.omTimer;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

public class DateUtilsTest {

    @Test
    public void should_return_correct_date() {
        String original = "2010-12-03T10:15:30+02:00";
        ZonedDateTime dateFromString = DateUtils.dateFromISO8601(original);
        String fromDate = DateUtils.ISO8601FromDate(dateFromString, ZoneId.of("+02:00"));

        assertThat(fromDate, is(original));
    }

    @Test
    public void omTimer_skal_returnere_24_timer_fram_i_tid() {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime om24Timer = omTimer(24);

        assertNotEquals(now.getDayOfMonth(), om24Timer.getDayOfMonth());

    }

}