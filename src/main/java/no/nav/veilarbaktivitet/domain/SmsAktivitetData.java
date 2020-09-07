package no.nav.veilarbaktivitet.domain;

import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder
public class SmsAktivitetData {
    String aktorId;
    Long aktivitetId;
    Long aktivtetVersion;
    Date moteTidAktivitet;
    Date smsSendtMoteTid;

    public boolean skalSendeServicevarsel() {
        return !moteTidAktivitet.equals(smsSendtMoteTid);

    }
}
