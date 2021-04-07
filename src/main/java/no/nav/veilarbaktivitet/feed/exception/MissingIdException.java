package no.nav.veilarbaktivitet.feed.exception;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import no.nav.veilarbaktivitet.feed.common.FeedWebhookResponse;

public class MissingIdException extends WebApplicationException {

	public MissingIdException() {
		super(
			Response
				.status(BAD_REQUEST)
				.entity(new FeedWebhookResponse().setMelding("Request må inneholde id"))
				.build()
		);
	}
}
