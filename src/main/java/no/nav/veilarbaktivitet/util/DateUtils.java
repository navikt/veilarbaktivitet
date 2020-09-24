package no.nav.veilarbaktivitet.util;

import lombok.SneakyThrows;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;

import static java.util.Optional.ofNullable;

public class DateUtils {


    private static final DatatypeFactory datatypeFactory = getDatatypeFactory();

    @SneakyThrows
    private static DatatypeFactory getDatatypeFactory() {
        return DatatypeFactory.newInstance();
    }

    public static XMLGregorianCalendar xmlCalendar(ZonedDateTime date) {
        return ofNullable(date).map(d->{
            GregorianCalendar cal = GregorianCalendar.from(date);
            return datatypeFactory.newXMLGregorianCalendar(cal);
        }).orElse(null);
    }

    public static ZonedDateTime getDate(XMLGregorianCalendar xmlGregorianCalendar){
        return ofNullable(xmlGregorianCalendar)
                .map(XMLGregorianCalendar::toGregorianCalendar)
                .map(GregorianCalendar::toZonedDateTime)
                .orElse(null);
    }

    public static ZonedDateTime dateFromISO8601(String date) {
        return ZonedDateTime.parse(date);
    }

    public static String ISO8601FromDate(ZonedDateTime date) {
        return ISO8601FromDate(date, ZoneId.systemDefault());
    }

    public static String ISO8601FromDate(ZonedDateTime date, ZoneId zoneId) {
        return ZonedDateTime.ofInstant(date.toInstant(), zoneId).toString();
    }

    public static XMLGregorianCalendar mergeDateTime(XMLGregorianCalendar date, XMLGregorianCalendar time) {
        if (date != null && time != null) {
            date.setHour(time.getHour());
            date.setMinute(time.getMinute());
            date.setSecond(time.getSecond());
        }
        return date;
    }

    public static ZonedDateTime omTimer(int timer) {
        ZonedDateTime now = ZonedDateTime.now();
        return now.plusHours(timer);
    }

}
