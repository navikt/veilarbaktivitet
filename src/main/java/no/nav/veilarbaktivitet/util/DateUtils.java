package no.nav.veilarbaktivitet.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.GregorianCalendar;

import static java.util.Optional.ofNullable;

@Slf4j
public class DateUtils {

    private static final String DATO_FORMAT = "yyyy-MM-dd";

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

    public static Date getDate(XMLGregorianCalendar xmlGregorianCalendar){
        return ofNullable(xmlGregorianCalendar)
                .map(XMLGregorianCalendar::toGregorianCalendar)
                .map(GregorianCalendar::getTime)
                .orElse(null);
    }

    public static Date dateFromISO8601(String date) {
        Instant instant =  ZonedDateTime.parse(date).toInstant();
        return Date.from(instant);
    }

    public static Date parseDato(String konfigurertDato) {
        try {
            return new SimpleDateFormat(DATO_FORMAT).parse(konfigurertDato);
        } catch (Exception e) {
            log.warn("Kunne ikke parse dato [{}] med datoformat [{}].", konfigurertDato, DATO_FORMAT);
            return null;
        }
    }

    public static String ISO8601FromDate(Date date) {
        return ISO8601FromDate(date, ZoneId.systemDefault());
    }

    public static String ISO8601FromDate(Date date, ZoneId zoneId) {
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

    public static Date omTimer(int timer) {
        long time = new Date().getTime();
        return new Date(time + timer * 1000 * 60 * 60);
    }

}
