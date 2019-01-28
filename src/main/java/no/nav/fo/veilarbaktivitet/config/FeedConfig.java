package no.nav.fo.veilarbaktivitet.config;


import no.nav.brukerdialog.security.oidc.OidcFeedAuthorizationModule;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.controller.FeedController;
import no.nav.fo.feed.producer.FeedProducer;
import no.nav.fo.veilarbaktivitet.db.dao.AktivitetFeedDAO;
import no.nav.fo.veilarbaktivitet.domain.AktivitetFeedData;
import no.nav.fo.veilarbaktivitet.feed.producer.AktivitetFeedProvider;
import no.nav.fo.veilarboppfolging.rest.domain.AvsluttetOppfolgingFeedDTO;
import no.nav.fo.veilarboppfolging.rest.domain.KvpDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeedConfig {

    public static boolean isMasterNode() {
        String isMasterString = System.getProperty("cluster.ismasternode", "false");
        return Boolean.parseBoolean(isMasterString);
    }

    @Bean
    public FeedController feedController(
            FeedProducer<AktivitetFeedData> aktivitetFeed,
            FeedConsumer<AvsluttetOppfolgingFeedDTO> avsluttetOppfolgingFeedItemFeedConsumer,
            FeedConsumer<KvpDTO> kvpFeedItemFeedConsumer
    ) {
        FeedController feedController = new FeedController();

        feedController.addFeed(AktivitetFeedData.FEED_NAME, aktivitetFeed);

        if (isMasterNode()) { // todo. this needs to be rewritten
            feedController.addFeed(AvsluttetOppfolgingFeedDTO.FEED_NAME, avsluttetOppfolgingFeedItemFeedConsumer);
            feedController.addFeed(KvpDTO.FEED_NAME, kvpFeedItemFeedConsumer);
        }

        return feedController;
    }

    @Bean
    public FeedProducer<AktivitetFeedData> aktivitetFeed(AktivitetFeedDAO aktivitetFeedDAO) {
        return FeedProducer.<AktivitetFeedData>builder()
                .provider(new AktivitetFeedProvider(aktivitetFeedDAO))
                .maxPageSize(1000)
                .authorizationModule(new OidcFeedAuthorizationModule())
                .build();
    }
}
