package no.nav.veilarbaktivitet.feed.controller;

import no.nav.veilarbaktivitet.feed.common.Authorization;
import no.nav.veilarbaktivitet.feed.consumer.FeedConsumer;
import org.slf4j.Logger;

import javax.ws.rs.HEAD;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;


@Path("/api/feed")
public class FeedController {

    private static final Logger LOG = getLogger(FeedController.class);

    private Map<String, FeedConsumer> consumers = new HashMap<>();


    public <DOMAINOBJECT extends Comparable<DOMAINOBJECT>> FeedController addFeed(String clientFeedname, FeedConsumer<DOMAINOBJECT> consumer) {
        LOG.info("ny feed-klient. navn={}", clientFeedname);
        consumers.put(clientFeedname, consumer);
        return this;
    }

    public FeedController() {
        LOG.info("starter");
    }

    // CONSUMER CONTROLLER

    @HEAD
    @Path("{name}")
    public Response webhookCallback(@PathParam("name") String feedname) {
        return ofNullable(feedname)
                .map((name) -> consumers.get(name))
                .map((consumer) -> authorizeRequest(consumer, feedname))
                .map(FeedConsumer::webhookCallback)
                .map((hadCallback) -> Response.status(hadCallback ? 200 : 404))
                .orElse(Response.status(404))
                .build();
    }

    private <T extends Authorization> T authorizeRequest(T feed, String name) {
        if (!feed.getAuthorizationModule().isRequestAuthorized(name)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        return feed;
    }

}
