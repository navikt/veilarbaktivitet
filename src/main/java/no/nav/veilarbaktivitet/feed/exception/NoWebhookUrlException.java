package no.nav.veilarbaktivitet.feed.exception;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import no.nav.veilarbaktivitet.feed.common.FeedWebhookResponse;

public class NoWebhookUrlException extends WebApplicationException {

	public NoWebhookUrlException() {
		super(
			Response
				.status(NOT_FOUND)
				.entity(
					new FeedWebhookResponse().setMelding("Ingen webhook-url er satt")
				)
				.build()
		);
	}
}
