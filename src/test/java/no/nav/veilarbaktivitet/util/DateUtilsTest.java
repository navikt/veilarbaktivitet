package no.nav.veilarbaktivitet.util;

import lombok.SneakyThrows;
import org.junit.Test;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static no.nav.veilarbaktivitet.util.DateUtils.omTimer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class DateUtilsTest {

    @Test
    public void omTimer_24_returnererNesteDag() {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime om24Timer = omTimer(24);

        assertNotEquals(now.getDayOfMonth(), om24Timer.getDayOfMonth());

    }

    @SneakyThrows
    @Test
    public void getDate_utenKlokkeslett_returnererKlokka00() {
        XMLGregorianCalendar cal = DatatypeFactory.newInstance().newXMLGregorianCalendarDate(2020, 10, 7, 2);
        ZonedDateTime date = DateUtils.getDate(cal);

        assertEquals(cal.getYear(), date.getYear());
        assertEquals(cal.getDay(), date.getDayOfMonth());
        assertEquals(cal.getMonth(), date.getMonthValue());
        assertEquals(0, date.getHour());
        assertEquals(0, date.getMinute());
        assertEquals(ZoneId.systemDefault(), date.getZone());
    }

    @SneakyThrows
    @Test
    public void mergeDateTime_mergerDatoOgKlokkeslett() {
        XMLGregorianCalendar date = DatatypeFactory.newInstance().newXMLGregorianCalendarDate(2020, 10, 7, 2);
        XMLGregorianCalendar time = DatatypeFactory.newInstance().newXMLGregorianCalendarTime(13, 11, 11, 2);

        XMLGregorianCalendar datetime = DateUtils.mergeDateTime(date, time);

        assertEquals(datetime.getYear(), date.getYear());
        assertEquals(datetime.getDay(), date.getDay());
        assertEquals(datetime.getMonth(), date.getMonth());
        assertEquals(datetime.getHour(), time.getHour());
        assertEquals(datetime.getMinute(), time.getMinute());
    }

    @SneakyThrows
    @Test
    public void getDate_medMergetKlokkeslett_returnererMedKlokkeslett() {
        XMLGregorianCalendar date = DatatypeFactory.newInstance().newXMLGregorianCalendarDate(2020, 10, 7, 2);
        XMLGregorianCalendar time = DatatypeFactory.newInstance().newXMLGregorianCalendarTime(13, 11, 11, 2);

        XMLGregorianCalendar mergedDateTime = DateUtils.mergeDateTime(date, time);
        ZonedDateTime datetime = DateUtils.getDate(mergedDateTime);

        assertEquals(mergedDateTime.getYear(), datetime.getYear());
        assertEquals(mergedDateTime.getDay(), datetime.getDayOfMonth());
        assertEquals(mergedDateTime.getMonth(), datetime.getMonthValue());
        assertEquals(mergedDateTime.getHour(), datetime.getHour());
        assertEquals(mergedDateTime.getMinute(), datetime.getMinute());
    }

}