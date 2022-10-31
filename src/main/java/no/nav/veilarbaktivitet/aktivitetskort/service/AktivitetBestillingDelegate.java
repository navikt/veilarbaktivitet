package no.nav.veilarbaktivitet.aktivitetskort.service;

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;

public abstract class AktivitetsBestillingDelegate<BestillingsType> {
    public abstract AktivitetData opprettAktivitet(BestillingsType bestilling);
}
