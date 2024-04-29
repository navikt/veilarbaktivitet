package no.nav.veilarbaktivitet.util;

import lombok.NonNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

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

    public static String iso8601DateFromZonedDateTime(ZonedDateTime date, ZoneId zoneId) {
        return OffsetDateTime.ofInstant(date.toInstant(), zoneId).format(iso8601DatoFormat);
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

    public static LocalDate dateToLocalDate(Date date) {
        if (date == null) return null;
        return LocalDate.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    public static LocalDateTime dateToLocalDateTime(java.util.Date date) {
        if (date == null) return null;
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    public static ZonedDateTime dateToZonedDateTime(Date date) {
        if (date == null) return null;
        return dateToLocalDateTime(date).atZone(ZoneId.systemDefault());
    }

    public static String klokkeslett(Date date) {
        if (date == null) return "";
        return dateToZonedDateTime(date).format(norskKlokkeslettformat);
    }

    public static String klokkeslett(ZonedDateTime date) {
        if (date == null) return "";
        return date.withZoneSameInstant(ZoneId.systemDefault()).format(norskKlokkeslettformat);
    }

    public static String norskDato(Date date) {
        if (date == null) return "";
        return dateToLocalDateTime(date).format(norskDatoformat);
    }

    public static String norskDato(ZonedDateTime date) {
        if (date == null) return "";
        return date.withZoneSameInstant(ZoneId.systemDefault()).format(norskDatoformat);
    }

    private static DateTimeFormatter norskDatoformat = DateTimeFormatter.ofPattern("d. MMMM uuuu", Locale.forLanguageTag("no"));
    private static DateTimeFormatter norskKlokkeslettformat = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("no"));
    private static DateTimeFormatter iso8601DatoFormat = DateTimeFormatter.ofPattern("YYYY-MM-dd");
}
