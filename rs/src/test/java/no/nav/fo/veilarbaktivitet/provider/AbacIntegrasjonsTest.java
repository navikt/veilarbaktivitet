package no.nav.fo.veilarbaktivitet.provider;

import no.nav.apiapp.security.PepClient;
import no.nav.apiapp.security.PepClientTester;
import no.nav.fo.IntegrasjonsTest;

import javax.inject.Inject;


public class AbacIntegrasjonsTest extends IntegrasjonsTest implements PepClientTester {

    @Inject
    private PepClient pepClient;

    @Override
    public PepClient getPepClient() {
        return pepClient;
    }

}