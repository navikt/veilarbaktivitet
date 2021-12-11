package no.nav.veilarbaktivitet;


import com.github.tomakehurst.wiremock.WireMockServer;
import no.nav.veilarbaktivitet.db.testdriver.TestDriver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import java.util.Properties;

@EnableAutoConfiguration
public class VeilarbAktivitetTestApp {

    public static void main(String[] args) {
        TestDriver.init();

        WireMockServer wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        int port = wireMockServer.port();

        Properties properties = new Properties();
        properties.put("wiremock.server.port", port);


        SpringApplication application = new SpringApplication(VeilarbaktivitetApp.class);
        application.setDefaultProperties(properties);
        application.run(args);
    }
}
