package no.nav.veilarbaktivitet.mock;

import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.client.aktorregister.IdentOppslag;
import no.nav.common.health.HealthCheckResult;

import java.util.List;
import java.util.stream.Collectors;


public class AktorregisterClientMock implements AktorregisterClient {

    @Override
    public String hentFnr(String aktorId) {
        return TestData.KJENT_IDENT.get();
    }

    @Override
    public String hentAktorId(String fnr) {
        return TestData.KJENT_AKTOR_ID.get();
    }

    @Override
    public List<IdentOppslag> hentFnr(List<String> list) {
        return list.stream()
                .map(aktorId -> new IdentOppslag(aktorId, aktorId + "fnr"))
                .collect(Collectors.toList());
    }

    @Override
    public List<IdentOppslag> hentAktorId(List<String> list) {
        return list.stream()
                .map(fnr -> new IdentOppslag(fnr, fnr + "aktorId"))
                .collect(Collectors.toList());
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckResult.healthy();
    }
}
