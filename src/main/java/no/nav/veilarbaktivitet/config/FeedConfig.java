package no.nav.veilarbaktivitet.config;

import no.nav.veilarbaktivitet.domain.AvsluttetOppfolgingFeedDTO;
import no.nav.veilarbaktivitet.domain.KvpDTO;
import no.nav.veilarbaktivitet.feed.consumer.FeedConsumer;
import no.nav.veilarbaktivitet.feed.consumer.FeedConsumerConfig;
import no.nav.veilarbaktivitet.feed.controller.FeedController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class FeedConfig {

    @Bean
    public FeedController feedController(
            FeedConsumer<AvsluttetOppfolgingFeedDTO> avsluttetOppfolgingFeedItemFeedConsumer,
            FeedConsumer<KvpDTO> kvpFeedItemFeedConsumer
    ) {
        FeedController feedController = new FeedController();

        feedController.addFeed(AvsluttetOppfolgingFeedDTO.FEED_NAME, avsluttetOppfolgingFeedItemFeedConsumer);
        feedController.addFeed(KvpDTO.FEED_NAME, kvpFeedItemFeedConsumer);

        return feedController;
    }

    @Bean
    public FeedConsumer<KvpDTO> kvpDTOFeedConsumer(KvpFeedConsumer kvpFeedConsumer) {
        FeedConsumerConfig.BaseConfig<KvpDTO> baseConfig = new FeedConsumerConfig.BaseConfig<>(
                KvpDTO.class,
                kvpFeedConsumer::sisteKjenteKvpId,
                getRequiredProperty(ApplicationContext.VEILARBOPPFOLGINGAPI_URL_PROPERTY),
                KvpDTO.FEED_NAME
        );

        FeedConsumerConfig<KvpDTO> config = new FeedConsumerConfig<>(baseConfig, new FeedConsumerConfig.SimplePollingConfig(10))
                .callback(kvpFeedConsumer::lesKvpFeed)
                .interceptors(Collections.singletonList(new OidcFeedOutInterceptor()));

        return new FeedConsumer<>(config);
    }

    @Bean
    public FeedConsumer<AvsluttetOppfolgingFeedDTO> avsluttetOppfolgingFeedItemFeedConsumer(AvsluttetOppfolgingFeedConsumer avsluttetOppfolgingFeedConsumer, LockProvider lockProvider) {
        BaseConfig<AvsluttetOppfolgingFeedDTO> baseConfig = new BaseConfig<>(
                AvsluttetOppfolgingFeedDTO.class,
                avsluttetOppfolgingFeedConsumer::sisteKjenteId,
                getRequiredProperty(VEILARBOPPFOLGINGAPI_URL_PROPERTY),
                AvsluttetOppfolgingFeedDTO.FEED_NAME
        );

        FeedConsumerConfig<AvsluttetOppfolgingFeedDTO> config = new FeedConsumerConfig<>(baseConfig, new SimplePollingConfig(10))
                .lockProvider(lockProvider, LOCK_HOLDING_LIMIT_IN_MS)
                .callback(avsluttetOppfolgingFeedConsumer::lesAvsluttetOppfolgingFeed)
                .interceptors(Collections.singletonList(new OidcFeedOutInterceptor()));

        return new FeedConsumer<>(config);
    }
}
