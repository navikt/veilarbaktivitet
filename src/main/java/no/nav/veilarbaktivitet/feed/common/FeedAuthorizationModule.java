package no.nav.veilarbaktivitet.feed.common;

@FunctionalInterface
public interface FeedAuthorizationModule {
    boolean isRequestAuthorized(String feedname);
}
