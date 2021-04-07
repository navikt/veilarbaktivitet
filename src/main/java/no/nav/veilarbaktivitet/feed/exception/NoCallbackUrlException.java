package no.nav.veilarbaktivitet.feed.exception;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import no.nav.veilarbaktivitet.feed.common.FeedWebhookResponse;

public class NoCallbackUrlException extends WebApplicationException {

	public NoCallbackUrlException() {
		super(
			Response
				.status(BAD_REQUEST)
				.entity(
					new FeedWebhookResponse()
					.setMelding("Request må inneholde webhookCallback-url")
				)
				.build()
		);
	}
}
