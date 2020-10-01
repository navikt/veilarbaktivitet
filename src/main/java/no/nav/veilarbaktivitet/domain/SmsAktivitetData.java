package no.nav.veilarbaktivitet.domain;

import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;

@Value
@Builder
public class SmsAktivitetData {
    String aktorId;
    Long aktivitetId;
    Long aktivtetVersion;
    ZonedDateTime moteTidAktivitet;
    ZonedDateTime smsSendtMoteTid;
    String aktivitetKanal;
    String smsKanal;

    public boolean skalSendeServicevarsel() {
        return !moteTidAktivitet.equals(smsSendtMoteTid) || !sendtMoteType(aktivitetKanal).equals(sendtMoteType(smsKanal));
    }

    public String moteType() {
        return sendtMoteType(aktivitetKanal);
    }

    private String sendtMoteType(String kanal) {
        if(KanalDTO.INTERNETT.toString().equals(kanal)) {
            return "videomøte";
        }
        if(KanalDTO.TELEFON.toString().equals(kanal)) {
            return "telefonmøte";
        }
        return "møte";
    }

    public String formatertMoteTid() {
        return formattertDateTime(moteTidAktivitet, DATO_FORMAT) + " kl. " + formattertDateTime(moteTidAktivitet, KLOKKE_FORMAT);
    }

    public String url() {
        return AKTIVITETSPLAN_URL +  "/aktivitet/vis/" + aktivitetId;
    }

    private String formattertDateTime(ZonedDateTime dateTime, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern).withLocale(norge);
        return dateTime.format(formatter);
    }

    private static final java.util.Locale norge = new java.util.Locale("no");

    private static final String DATO_FORMAT = "d. MMMM yyyy";
    private static final String KLOKKE_FORMAT = "HH:mm";
    private static final String AKTIVITETSPLAN_URL = getRequiredProperty("AKTIVITETSPLAN_URL");

}
