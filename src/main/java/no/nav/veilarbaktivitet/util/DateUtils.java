package no.nav.veilarbaktivitet.util;

import lombok.NonNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.Date;

public class DateUtils {

    private DateUtils() {}

    public static Date toDate(LocalDate localDate) {
        if (localDate == null) return null;
        return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }

    public static Date dateFromISO8601(String date) {
        Instant instant =  ZonedDateTime.parse(date).toInstant();
        return Date.from(instant);
    }

    public static String iso8601Fromdate(Date date, ZoneId zoneId) {
        return OffsetDateTime.ofInstant(date.toInstant(), zoneId).toString();
    }

    /**
     *
     * @param dateString dato-streng
     * @param dateFormat Datoformat for parsing, f.eks "yyyy-MM-dd HH:mm:ss"
     * @return Dato objektet
     * @throws ParseException ved feil i formatet
     */
    public static Date dateFromString(String dateString, @NonNull SimpleDateFormat dateFormat) throws ParseException {
        return dateFormat.parse(dateString);
    }

    /**
     *
     * @param dateString dato-streng, på formatet "yyyy-MM-dd HH:mm:ss"
     * @return Dato objektet
     * @throws ParseException ved feil i formatet
     */
    public static Date dateFromString(String dateString) throws ParseException {
        return dateFromString(dateString, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
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
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.of("Europe/Oslo"));
    }

    public static ZonedDateTime dateToZonedDateTime(Date date) {
        if (date == null) return null;
        return dateToLocalDateTime(date).atZone(ZoneId.of("Europe/Oslo"));
    }
}
