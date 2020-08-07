package no.nav.veilarbaktivitet.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.env")
public class EnvironmentProperties {
    private String openAmDiscoveryUrl;
    private String openAmClientId;
    private String openAmRefreshUrl;
    private String azureAdDiscoveryUrl;
    private String azureAdClientId;
    private String azureAdB2cDiscoveryUrl;
    private String azureAdB2cClientId;
    private String naisStsDiscoveryUrl;
    private String abacUrl;
    private String aktorregisterUrl;
    private String dbUrl;
    private String kafkaBrokersUrl;
    private String environmentName;
    private String cxfStsUrl;
}
