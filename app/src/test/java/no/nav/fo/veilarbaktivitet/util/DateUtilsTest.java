package no.nav.fo.veilarbaktivitet.util;

import org.junit.Test;

import java.time.ZoneId;
import java.util.Date;

import static no.nav.fo.veilarbaktivitet.util.DateUtils.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class DateUtilsTest {

    @Test
    public void should_return_correct_date() {
        String original = "2010-12-03T10:15:30+02:00";
        Date dateFromString = dateFromISO8601(original);
        String fromDate = ISO8601FromDate(dateFromString, ZoneId.of("+02:00"));

        assertThat(fromDate, is(original));
    }

}