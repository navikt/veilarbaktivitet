package no.nav.fo.veilarbaktivitet.config;

import no.nav.brukerdialog.security.oidc.OidcFeedOutInterceptor;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.consumer.FeedConsumerConfig;
import no.nav.fo.veilarbsituasjon.rest.domain.AvsluttetOppfolgingFeedDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class AvsluttetOppfolgingFeedConfig {

    @Value("${avsluttoppfolging.feed.producer.url}")
    private String host;

    @Value("${avsluttoppfolging.feed.consumer.pollingrate}")
    private String polling;

    @Bean
    public FeedConsumer<AvsluttetOppfolgingFeedDTO> avsluttetOppfolgingFeedItemFeedConsumer(AvsluttetOppfolgingFeedProvider avsluttetOppfolgingFeedProvider) {
        FeedConsumerConfig<AvsluttetOppfolgingFeedDTO> config = new FeedConsumerConfig<>(
                AvsluttetOppfolgingFeedDTO.class,
                avsluttetOppfolgingFeedProvider::sisteEndring,
                host,
                AvsluttetOppfolgingFeedDTO.FEED_NAME
        )
                .pollingInterval(polling)
                .callback(avsluttetOppfolgingFeedProvider::lesAvsluttetOppfolgingFeed)
                .interceptors(Collections.singletonList(new OidcFeedOutInterceptor()));

        return new FeedConsumer<>(config);
    }
}
