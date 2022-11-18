package no.nav.veilarbaktivitet.util;

import lombok.SneakyThrows;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.*;
import java.util.Date;
import java.util.GregorianCalendar;

import static java.util.Optional.ofNullable;

public class DateUtils {
    private DateUtils() {}

    private static final DatatypeFactory datatypeFactory = getDatatypeFactory();

    @SneakyThrows
    private static DatatypeFactory getDatatypeFactory() {
        return DatatypeFactory.newInstance();
    }

    public static XMLGregorianCalendar xmlCalendar(Date date) {
        return ofNullable(date).map(d->{
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(date);
            return datatypeFactory.newXMLGregorianCalendar(cal);
        }).orElse(null);
    }

    public static Date toDate(LocalDate localDate) {
        if (localDate == null) return null;
        return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }

    public static Date dateFromISO8601(String date) {
        Instant instant =  ZonedDateTime.parse(date).toInstant();
        return Date.from(instant);
    }

    public static String ISO8601FromDate(Date date, ZoneId zoneId) {
        return ZonedDateTime.ofInstant(date.toInstant(), zoneId).toString();
    }

    public static OffsetDateTime toOffsetDateTime(Date date) {
        if (date == null) return null;
        return date.toInstant().atOffset(ZoneOffset.UTC);
    }

    public static LocalDate toLocalDate(Date date) {
        if (date == null) return null;
        return date.toInstant().atOffset(ZoneOffset.UTC).toLocalDate();
    }

    public static Date localDateTimeToDate(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static Date zonedDateTimeToDate(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) return null;
        return localDateTimeToDate(zonedDateTime.toLocalDateTime());
    }

    public static LocalDateTime dateToLocalDateTime(Date date) {
        if (date == null) return null;
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    public static ZonedDateTime dateToZonedDateTime(Date date) {
        if (date == null) return null;
        return dateToLocalDateTime(date).atZone(ZoneId.systemDefault());
    }

}
