package no.nav.veilarbaktivitet.feed.exception;

import no.nav.pto.veilarbportefolje.feed.common.FeedWebhookResponse;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

public class InvalidUrlException extends WebApplicationException{
    public InvalidUrlException() {
        super(
                Response
                        .status(BAD_REQUEST)
                        .entity(new FeedWebhookResponse().setMelding("Feil format på webhookCallback-url"))
                        .build()
        );
    }
}
