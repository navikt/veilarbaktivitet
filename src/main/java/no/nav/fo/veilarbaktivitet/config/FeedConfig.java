package no.nav.fo.veilarbaktivitet.config;


import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider;
import no.nav.brukerdialog.security.oidc.OidcFeedAuthorizationModule;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.controller.FeedController;
import no.nav.fo.feed.producer.FeedProducer;
import no.nav.fo.veilarbaktivitet.db.dao.AktivitetFeedDAO;
import no.nav.fo.veilarbaktivitet.domain.AktivitetFeedData;
import no.nav.fo.veilarbaktivitet.domain.AvsluttetOppfolgingFeedDTO;
import no.nav.fo.veilarbaktivitet.domain.KvpDTO;
import no.nav.fo.veilarbaktivitet.feed.producer.AktivitetFeedProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Inject;
import javax.sql.DataSource;

@Configuration
public class FeedConfig {

    @Inject
    private DataSource dataSource;

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcLockProvider(dataSource);
    }


    @Bean
    public FeedController feedController(
            FeedProducer<AktivitetFeedData> aktivitetFeed,
            FeedConsumer<AvsluttetOppfolgingFeedDTO> avsluttetOppfolgingFeedItemFeedConsumer,
            FeedConsumer<KvpDTO> kvpFeedItemFeedConsumer
    ) {
        FeedController feedController = new FeedController();

        feedController.addFeed(AktivitetFeedData.FEED_NAME, aktivitetFeed);

        feedController.addFeed(AvsluttetOppfolgingFeedDTO.FEED_NAME, avsluttetOppfolgingFeedItemFeedConsumer);
        feedController.addFeed(KvpDTO.FEED_NAME, kvpFeedItemFeedConsumer);


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
