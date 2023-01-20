package no.nav.veilarbaktivitet;


import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.veilarbaktivitet.db.testdriver.TestDriver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import java.util.Properties;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

@EnableAutoConfiguration
public class InternVeilarbAktivitetTestApp {

    public static void main(String[] args) {
        TestDriver.init();

        WireMockServer wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        int port = wireMockServer.port();

        Properties properties = new Properties();
        properties.put("wiremock.server.port", port);

        wireMockServer.stubFor(
                WireMock.get(urlMatching("/veilarboppfolging/.*"))
                        .willReturn(ok())
        );

        SpringApplication application = new SpringApplication(VeilarbaktivitetApp.class);
        application.setDefaultProperties(properties);
        application.setAdditionalProfiles("intern");
        application.run(args);
    }
}
