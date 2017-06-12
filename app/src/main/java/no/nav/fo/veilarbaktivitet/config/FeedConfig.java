package no.nav.fo.veilarbaktivitet.config;


import no.nav.fo.feed.controller.FeedController;
import no.nav.fo.feed.producer.FeedProducer;
import no.nav.fo.veilarbaktivitet.db.dao.AktivitetFeedDAO;
import no.nav.fo.veilarbaktivitet.domain.AktivitetFeedData;
import no.nav.fo.veilarbaktivitet.feed.producer.AktivitetFeedProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class FeedConfig {

    @Bean
    public FeedController feedController(FeedProducer<AktivitetFeedData> aktivitetFeed) {
        FeedController feedServerController = new FeedController();

        feedServerController.addFeed("aktiviteter", aktivitetFeed);

        return feedServerController;
    }

    @Bean
    public FeedProducer<AktivitetFeedData> aktivitetFeed(AktivitetFeedDAO aktivitetFeedDAO) {
        return FeedProducer.<AktivitetFeedData>builder()
                .provider(new AktivitetFeedProvider(aktivitetFeedDAO))
                .maxPageSize(1000)
                .build();
    }
}
