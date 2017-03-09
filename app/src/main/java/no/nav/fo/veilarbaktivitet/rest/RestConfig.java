package no.nav.fo.veilarbaktivitet.rest;


import org.glassfish.jersey.server.ResourceConfig;

public class RestConfig extends ResourceConfig {

    public RestConfig() {
        super(
                JsonProvider.class,
                RestService.class
        );
    }

}