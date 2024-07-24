package no.nav.veilarbaktivitet.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.env")
public class EnvironmentProperties {
    private String abacUrl;
    private String kafkaBrokersUrl;
    private String environmentName;
    private String cxfStsUrl;
    private String naisAadDiscoveryUrl;
    private String naisAadClientId;
}
