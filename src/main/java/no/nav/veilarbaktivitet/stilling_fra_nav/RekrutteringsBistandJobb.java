package no.nav.veilarbaktivitet.stilling_fra_nav;

import no.nav.veilarbaktivitet.send_paa_kafka.AktivitetsJobb;
import no.nav.veilarbaktivitet.send_paa_kafka.JobbType;

public class RekrutteringsBistandJobb implements AktivitetsJobb {

    public void behandleAktivitet(long aktivitetId, long aktivitetVersjon) {
        // hente aktiviteten
        // finne ut hva slags melding som skal sendes
        // ....
        // sende melding
    }

    @Override
    public JobbType getJobbType() {
        return JobbType.REKRUTTERINGSBISTAND_KAFKA;
    }
}
