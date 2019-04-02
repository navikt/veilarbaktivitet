package no.nav.fo.veilarbaktivitet.config;

import no.nav.apiapp.security.veilarbabac.VeilarbAbacPepClient;
import no.nav.brukerdialog.security.oidc.SystemUserTokenProvider;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.context.AbacContext;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;

@Configuration
@Import({AbacContext.class})
public class AbacConfig {

    private SystemUserTokenProvider systemUserTokenProvider = new SystemUserTokenProvider();

    @Inject
    UnleashService unleashService;

    @Bean
    public VeilarbAbacPepClient pepClient(Pep pep) {

        return VeilarbAbacPepClient.ny()
                .medPep(pep)
                .medSystemUserTokenProvider(()->systemUserTokenProvider.getToken())
                .brukAktoerId(()->unleashService.isEnabled("veilarbaktivitet.veilarbabac.aktor"))
                .sammenlikneTilgang(()->unleashService.isEnabled("veilarbaktivitet.veilarbabac.sammenlikn"))
                .foretrekkVeilarbAbacResultat(()->unleashService.isEnabled("veilarbaktivitet.veilarbabac.foretrekk_veilarbabac"))
                .bygg();
    }

}