package no.nav.veilarbaktivitet.aktivitet.domain;

import no.nav.veilarbaktivitet.util.EnumUtils;

public enum StillingsoekEtikettData {
    INGEN("Ingen"),
    SOKNAD_SENDT("Søknaden er sendt"),
    INNKALT_TIL_INTERVJU("Skal på intervju"),
    AVSLAG("Ikke fått jobben"),
    JOBBTILBUD("Fått jobbtilbud");

    public final String text;

    StillingsoekEtikettData(String text) {
        this.text = text;
    }

    public static StillingsoekEtikettData fraString(String s) {
        var nullableEtikettData = EnumUtils.valueOf(StillingsoekEtikettData.class, s);
        return nullableEtikettData == null ? INGEN : nullableEtikettData;
    }
}
