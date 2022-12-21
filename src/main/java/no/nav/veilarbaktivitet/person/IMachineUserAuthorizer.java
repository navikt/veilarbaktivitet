package no.nav.veilarbaktivitet.person;

import no.nav.common.auth.context.AuthContextHolder;

public interface IMachineUserAuthorizer {
    public boolean sjekkHarM2Mtilgang(AuthContextHolder authContextHolder);
}
