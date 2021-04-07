package no.nav.veilarbaktivitet.feed.exception;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import no.nav.veilarbaktivitet.feed.common.FeedWebhookResponse;

public class InvalidUrlException extends WebApplicationException {

	public InvalidUrlException() {
		super(
			Response
				.status(BAD_REQUEST)
				.entity(
					new FeedWebhookResponse()
					.setMelding("Feil format på webhookCallback-url")
				)
				.build()
		);
	}
}
