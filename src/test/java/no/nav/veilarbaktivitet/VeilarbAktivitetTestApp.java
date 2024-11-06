package no.nav.veilarbaktivitet;


import com.github.tomakehurst.wiremock.WireMockServer;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import no.nav.poao_tilgang.poao_tilgang_test_wiremock.PoaoTilgangWiremock;
import no.nav.veilarbaktivitet.mock_nav_modell.NavMockService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import java.util.Properties;

@EnableAutoConfiguration
@AutoConfigureEmbeddedDatabase
public class VeilarbAktivitetTestApp {
    private static final PoaoTilgangWiremock poaoTilgangWiremock = new PoaoTilgangWiremock(0, "", NavMockService.Companion.getNAV_CONTEXT());


    public static void main(String[] args) {
        WireMockServer wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        int port = wireMockServer.port();

        Properties properties = new Properties();
        properties.put("wiremock.server.port", port);
        properties.put("app.env.poao_tilgang.url", poaoTilgangWiremock.getWireMockServer().baseUrl());



        SpringApplication application = new SpringApplication(VeilarbaktivitetApp.class);
        application.setDefaultProperties(properties);
        application.run(args);
    }
}
