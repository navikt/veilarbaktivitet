import no.nav.apiapp.ApiApp;
import no.nav.brukerdialog.tools.SecurityConstants;
import no.nav.common.utils.NaisUtils;
import no.nav.fo.veilarbaktivitet.ApplicationContext;
import no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants;
import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;

import static java.lang.System.setProperty;
import static no.nav.brukerdialog.security.Constants.OIDC_REDIRECT_URL_PROPERTY_NAME;
import static no.nav.common.utils.NaisUtils.getCredentials;
import static no.nav.dialogarena.aktor.AktorConfig.AKTOER_ENDPOINT_URL;
import static no.nav.fo.veilarbaktivitet.ApplicationContext.*;
import static no.nav.fo.veilarbaktivitet.db.DatabaseContext.VEILARBAKTIVITETDATASOURCE_PASSWORD_PROPERTY;
import static no.nav.fo.veilarbaktivitet.db.DatabaseContext.VEILARBAKTIVITETDATASOURCE_USERNAME_PROPERTY;
import static no.nav.sbl.dialogarena.common.abac.pep.service.AbacServiceConfig.ABAC_ENDPOINT_URL_PROPERTY_NAME;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

public class Main {

    public static void main(String... args) {
        setProperty(ARENA_AKTIVITET_DATOFILTER_PROPERTY, "2017-12-04");

        NaisUtils.Credentials serviceUser = getCredentials("service_user");

        //ABAC
        System.setProperty(CredentialConstants.SYSTEMUSER_USERNAME, serviceUser.username);
        System.setProperty(CredentialConstants.SYSTEMUSER_PASSWORD, serviceUser.password);

        //CXF
        System.setProperty(StsSecurityConstants.SYSTEMUSER_USERNAME, serviceUser.username);
        System.setProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD, serviceUser.password);

        //OIDC
        System.setProperty(SecurityConstants.SYSTEMUSER_USERNAME, serviceUser.username);
        System.setProperty(SecurityConstants.SYSTEMUSER_PASSWORD, serviceUser.password);

        System.setProperty(StsSecurityConstants.STS_URL_KEY, getRequiredProperty(SECURITYTOKENSERVICE_URL));
        System.setProperty(AKTOER_ENDPOINT_URL, getRequiredProperty(AKTOER_V2_ENDPOINTURL));
        System.setProperty(OIDC_REDIRECT_URL_PROPERTY_NAME, getRequiredProperty(REDIRECT_URL_PROPERTY));
        System.setProperty(ABAC_ENDPOINT_URL_PROPERTY_NAME, getRequiredProperty(ABAC_PDP_ENDPOINT_URL));
        System.setProperty(AKTOERREGISTER_API_V1_URL, getRequiredProperty(AKTOERREGISTER_API_V1_URL));

        NaisUtils.Credentials oracleCreds = getCredentials("oracle_creds");
        System.setProperty(VEILARBAKTIVITETDATASOURCE_USERNAME_PROPERTY, oracleCreds.username);
        System.setProperty(VEILARBAKTIVITETDATASOURCE_PASSWORD_PROPERTY, oracleCreds.password);

        ApiApp.runApp(ApplicationContext.class, args);
    }
}
