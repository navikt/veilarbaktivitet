package no.nav.veilarbaktivitet.motesms.gammel;

import lombok.Builder;
import lombok.Value;
import no.nav.veilarbaktivitet.aktivitet.dto.KanalDTO;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

    private static final String AKTIVITETSPLAN_URL = getRequiredProperty("AKTIVITETSPLAN_URL");
    SimpleDateFormat formatter = new SimpleDateFormat("d. MMMM yyyy 'kl.' HH:mm", new Locale("no"));

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
        return formatter.format(moteTidAktivitet);
    }

    public String url() {
        return AKTIVITETSPLAN_URL +  "/aktivitet/vis/" + aktivitetId;
    }

}
