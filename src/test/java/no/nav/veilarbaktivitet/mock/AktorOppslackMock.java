package no.nav.veilarbaktivitet.mock;

import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AktorOppslackMock implements AktorOppslagClient {

    @Override
    public Fnr hentFnr(AktorId aktorId) {
        return Fnr.of(TestData.KJENT_IDENT.get());
    }

    @Override
    public AktorId hentAktorId(Fnr fnr) {
        return AktorId.of(TestData.KJENT_AKTOR_ID.get());
    }

    @Override
    public Map<AktorId, Fnr> hentFnrBolk(List<AktorId> list) {
        return Collections.emptyMap();
    }

    @Override
    public Map<Fnr, AktorId> hentAktorIdBolk(List<Fnr> list) {
        return Collections.emptyMap();
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckResult.healthy();
    }

}
