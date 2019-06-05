package no.nav.fo.veilarbaktivitet;

import no.nav.fasit.DbCredentials;
import no.nav.fasit.FasitUtils;
import no.nav.fasit.ServiceUser;
import no.nav.fo.veilarbaktivitet.service.VeilArbAbacService;
import no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants;

import static no.nav.brukerdialog.security.Constants.*;
import static no.nav.brukerdialog.security.oidc.provider.AzureADB2CConfig.EXTERNAL_USERS_AZUREAD_B2C_DISCOVERY_URL;
import static no.nav.fasit.FasitUtils.*;
import static no.nav.fasit.FasitUtils.Zone.FSS;
import static no.nav.fo.veilarbaktivitet.ApplicationContext.*;
import static no.nav.fo.veilarbaktivitet.db.DatabaseContext.*;
import static no.nav.fo.veilarbaktivitet.service.VeilArbAbacService.VEILARBABAC_HOSTNAME_PROPERTY;
import static no.nav.sbl.dialogarena.common.abac.pep.service.AbacServiceConfig.ABAC_ENDPOINT_URL_PROPERTY_NAME;
import static no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants.*;
import static no.nav.sbl.featuretoggle.unleash.UnleashServiceConfig.UNLEASH_API_URL_PROPERTY_NAME;
import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;
import static no.nav.sbl.util.EnvironmentUtils.setProperty;

public class TestContext {

    private static final String SERVICE_USER_ALIAS = "srvveilarbaktivitet";
    private static final String SECURITY_TOKEN_SERVICE_ALIAS = "securityTokenService";
    private static final String ABAC_PDP_ENDPOINT_ALIAS = "abac.pdp.endpoint";
    private static final String AKTOER_V2_ALIAS = "Aktoer_v2";
    private static final String VEIL_ARB_OPPFOLGING_API_ALIAS = "veilArbOppfolgingAPI";
    private static final String VEILARBLOGIN_REDIRECT_URL_ALIAS = "veilarblogin.redirect-url";
    private static final String AZURE_AD_B2C_DISCOVERY_ALIAS = "aad_b2c_discovery";
    private static final String AAD_B2C_CLIENTID_ALIAS = "aad_b2c_clientid";
    private static final String UNLEASH_API_ALIAS = "unleash-api";
    private static final String VIRKSOMHET_TILTAK_OG_AKTIVITET_V1_ALIAS = "virksomhet:TiltakOgAktivitet_v1";

    public static void setup() {
        ServiceUser serviceUser = getServiceUser(SERVICE_USER_ALIAS, APPLICATION_NAME);
        setProperty(SYSTEMUSER_USERNAME, serviceUser.getUsername(), PUBLIC);
        setProperty(SYSTEMUSER_PASSWORD, serviceUser.getPassword(), PUBLIC);

        DbCredentials dbCredentials = getDbCredentials(APPLICATION_NAME);
        setProperty(VEILARBAKTIVITETDATASOURCE_URL_PROPERTY, dbCredentials.getUrl(), PUBLIC);
        setProperty(VEILARBAKTIVITETDATASOURCE_USERNAME_PROPERTY, dbCredentials.getUsername(), PUBLIC);
        setProperty(VEILARBAKTIVITETDATASOURCE_PASSWORD_PROPERTY, dbCredentials.getPassword(), PUBLIC);

        setProperty(STS_URL_KEY, getBaseUrl(SECURITY_TOKEN_SERVICE_ALIAS, FSS), PUBLIC);
        setProperty(ABAC_ENDPOINT_URL_PROPERTY_NAME, getRestService(ABAC_PDP_ENDPOINT_ALIAS, getDefaultEnvironment()).getUrl(), PUBLIC);
        setProperty(CredentialConstants.SYSTEMUSER_USERNAME, serviceUser.getUsername(), PUBLIC);
        setProperty(CredentialConstants.SYSTEMUSER_PASSWORD, serviceUser.getPassword(), PUBLIC);
        setProperty(AKTOER_V2_URL_PROPERTY, getWebServiceEndpoint(AKTOER_V2_ALIAS).getUrl(), PUBLIC);
        setProperty(UNLEASH_API_URL_PROPERTY_NAME, "https://unleash.nais.adeo.no/api/", PUBLIC); // getRestService(UNLEASH_API_ALIAS, getDefaultEnvironment()).getUrl(), PUBLIC);

        setProperty(VEILARBOPPFOLGINGAPI_URL_PROPERTY, getRestService(VEIL_ARB_OPPFOLGING_API_ALIAS, getDefaultEnvironment()).getUrl(), PUBLIC);
        setProperty(VIRKSOMHET_TILTAKOGAKTIVITET_V1_ENDPOINTURL_PROPERTY, getWebServiceEndpoint(VIRKSOMHET_TILTAK_OG_AKTIVITET_V1_ALIAS, getDefaultEnvironment()).getUrl(), PUBLIC);

        ServiceUser isso_rp_user = getServiceUser("isso-rp-user", APPLICATION_NAME);
        String loginUrl = getRestService(VEILARBLOGIN_REDIRECT_URL_ALIAS, getDefaultEnvironment()).getUrl();
        setProperty(ISSO_HOST_URL_PROPERTY_NAME, getBaseUrl("isso-host"), PUBLIC);
        setProperty(ISSO_RP_USER_USERNAME_PROPERTY_NAME, isso_rp_user.getUsername(), PUBLIC);
        setProperty(ISSO_RP_USER_PASSWORD_PROPERTY_NAME, isso_rp_user.getPassword(), PUBLIC);
        setProperty(ISSO_JWKS_URL_PROPERTY_NAME, getBaseUrl("isso-jwks"), PUBLIC);
        setProperty(ISSO_ISSUER_URL_PROPERTY_NAME, getBaseUrl("isso-issuer"), PUBLIC);
        setProperty(ISSO_ISALIVE_URL_PROPERTY_NAME, getBaseUrl("isso.isalive", FSS), PUBLIC);
        setProperty(VEILARBLOGIN_REDIRECT_URL_URL_PROPERTY, loginUrl, PUBLIC);

        ServiceUser aadB2cUser = getServiceUser(AAD_B2C_CLIENTID_ALIAS, APPLICATION_NAME);
        setProperty(EXTERNAL_USERS_AZUREAD_B2C_DISCOVERY_URL, getBaseUrl(AZURE_AD_B2C_DISCOVERY_ALIAS), PUBLIC);
        setProperty(AAD_B2C_CLIENTID_USERNAME_PROPERTY, aadB2cUser.getUsername(), PUBLIC);
        setProperty(AAD_B2C_CLIENTID_PASSWORD_PROPERTY, aadB2cUser.getPassword(), PUBLIC);

        setProperty(VEILARBABAC_HOSTNAME_PROPERTY, "https://veilarbabac-" + FasitUtils.getDefaultEnvironment() + ".nais.preprod.local/", PUBLIC);
    }
}
