package no.nav.veilarbaktivitet.domain;

import lombok.Builder;
import lombok.Value;

import java.text.SimpleDateFormat;
import java.util.Date;

import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;

@Value
@Builder
public class SmsAktivitetData {
    String aktorId;
    Long aktivitetId;
    Long aktivtetVersion;
    Date moteTidAktivitet;
    Date smsSendtMoteTid;
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
        return DatoFormaterer.format(moteTidAktivitet) + " kl. " + KlokkeFormaterer.format(moteTidAktivitet);
    }

    public String url() {
        return AKTIVITETSPLAN_URL +  "/aktivitet/vis/" + aktivitetId;
    }

    private static final java.util.Locale norge = new java.util.Locale("no");
    private static final SimpleDateFormat DatoFormaterer = new SimpleDateFormat("d. MMMM yyyy", norge);
    private static final SimpleDateFormat KlokkeFormaterer = new SimpleDateFormat("HH:mm", norge);

    private static final String AKTIVITETSPLAN_URL = getRequiredProperty("AKTIVITETSPLAN_URL");

}
