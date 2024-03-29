package no.nav.veilarbaktivitet.manuell_status.v2;

import lombok.Value;

@Value
public class ManuellStatusV2DTO {
    boolean erUnderManuellOppfolging;
    KrrStatus krrStatus;

    @Value
    public static class KrrStatus {
        boolean kanVarsles;
        boolean erReservert;
    }
}
