package no.nav.fo.veilarbaktivitet.config;

import no.nav.brukerdialog.security.oidc.OidcFeedOutInterceptor;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.consumer.FeedConsumerConfig;
import no.nav.fo.veilarbaktivitet.feed.consumer.KvpFeedConsumer;
import no.nav.fo.veilarboppfolging.rest.domain.KvpDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class KvpFeedConfig {

    @Value("${veilarboppfolging.api.url}")
    private String host;

    @Value("${kvp.feed.consumer.pollingrate}")
    private String polling;

    @Bean
    public FeedConsumer<KvpDTO> KvpFeedConsumer(KvpFeedConsumer c) {
        FeedConsumerConfig<KvpDTO> config = new FeedConsumerConfig<>(
                new FeedConsumerConfig.BaseConfig<>(
                        KvpDTO.class,
                        c::lastSerial,
                        host,
                        KvpDTO.FEED_NAME
                ),
                new FeedConsumerConfig.PollingConfig(polling)
        )
                .callback(c::consume)
                .interceptors(Collections.singletonList(new OidcFeedOutInterceptor()));

        return new FeedConsumer<>(config);
    }
}

