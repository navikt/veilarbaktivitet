package no.nav.veilarbaktivitet.config;

import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.veilarbaktivitet.domain.AvsluttetOppfolgingFeedDTO;
import no.nav.veilarbaktivitet.domain.KvpDTO;
import no.nav.veilarbaktivitet.feed.AvsluttetOppfolgingFeedConsumer;
import no.nav.veilarbaktivitet.feed.KvpFeedConsumer;
import no.nav.veilarbaktivitet.feed.consumer.FeedConsumer;
import no.nav.veilarbaktivitet.feed.consumer.FeedConsumerConfig;
import no.nav.veilarbaktivitet.feed.controller.FeedController;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;

@Configuration
public class FeedConfig {

//    TODO: Skru av konsumering av feeder som et midlertidig steg før fjerning etter migrering til kafka
//    @Bean
//    public FeedController feedController(
//            FeedConsumer<AvsluttetOppfolgingFeedDTO> avsluttetOppfolgingFeedItemFeedConsumer,
//            FeedConsumer<KvpDTO> kvpFeedItemFeedConsumer
//    ) {
//        FeedController feedController = new FeedController();
//
//        feedController.addFeed(AvsluttetOppfolgingFeedDTO.FEED_NAME, avsluttetOppfolgingFeedItemFeedConsumer);
//        feedController.addFeed(KvpDTO.FEED_NAME, kvpFeedItemFeedConsumer);
//
//        return feedController;
//    }

    @Bean
    public FeedController feedController() {
        return new FeedController();
    }

    @Bean
    public FeedConsumer<KvpDTO> kvpDTOFeedConsumer(KvpFeedConsumer kvpFeedConsumer, OkHttpClient client, LeaderElectionClient leaderElectionClient) {
        FeedConsumerConfig.BaseConfig<KvpDTO> baseConfig = new FeedConsumerConfig.BaseConfig<>(
                KvpDTO.class,
                kvpFeedConsumer::sisteKjenteKvpId,
                getRequiredProperty(ApplicationContext.VEILARBOPPFOLGINGAPI_URL_PROPERTY),
                KvpDTO.FEED_NAME
        );

        FeedConsumerConfig<KvpDTO> config = new FeedConsumerConfig<>(baseConfig, new FeedConsumerConfig.SimplePollingConfig(10), leaderElectionClient)
                .restClient(client)
                .callback(kvpFeedConsumer::lesKvpFeed);

        return new FeedConsumer<>(config);
    }

    @Bean
    public FeedConsumer<AvsluttetOppfolgingFeedDTO> avsluttetOppfolgingFeedItemFeedConsumer(AvsluttetOppfolgingFeedConsumer avsluttetOppfolgingFeedConsumer, OkHttpClient client, LeaderElectionClient leaderElectionClient) {
        FeedConsumerConfig.BaseConfig<AvsluttetOppfolgingFeedDTO> baseConfig = new FeedConsumerConfig.BaseConfig<>(
                AvsluttetOppfolgingFeedDTO.class,
                avsluttetOppfolgingFeedConsumer::sisteKjenteId,
                getRequiredProperty(ApplicationContext.VEILARBOPPFOLGINGAPI_URL_PROPERTY),
                AvsluttetOppfolgingFeedDTO.FEED_NAME
        );

        FeedConsumerConfig<AvsluttetOppfolgingFeedDTO> config = new FeedConsumerConfig<>(baseConfig, new FeedConsumerConfig.SimplePollingConfig(10), leaderElectionClient)
                .restClient(client)
                .callback(avsluttetOppfolgingFeedConsumer::lesAvsluttetOppfolgingFeed);

        return new FeedConsumer<>(config);
    }
}
