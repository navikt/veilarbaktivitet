package no.nav.veilarbaktivitet.config;
import net.javacrumbs.shedlock.core.LockProvider;
import no.nav.brukerdialog.security.oidc.OidcFeedOutInterceptor;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.consumer.FeedConsumerConfig;
import no.nav.veilarbaktivitet.domain.KvpDTO;
import no.nav.veilarbaktivitet.feed.consumer.KvpFeedConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
public class KVPFeedConfig {

    private static final int LOCK_HOLDING_LIMIT_IN_MS = 10 * 60 * 1000;


    @Bean
    public FeedConsumer<KvpDTO> kvpDTOFeedConsumer(KvpFeedConsumer kvpFeedConsumer, LockProvider lockProvider) {
        FeedConsumerConfig.BaseConfig<KvpDTO> baseConfig = new FeedConsumerConfig.BaseConfig<>(
                KvpDTO.class,
                kvpFeedConsumer::sisteKjenteKvpId,
                getRequiredProperty(ApplicationContext.VEILARBOPPFOLGINGAPI_URL_PROPERTY),
                KvpDTO.FEED_NAME
        );

        FeedConsumerConfig<KvpDTO> config = new FeedConsumerConfig<>(baseConfig, new FeedConsumerConfig.SimplePollingConfig(10))
                .lockProvider(lockProvider, LOCK_HOLDING_LIMIT_IN_MS)
                .callback(kvpFeedConsumer::lesKvpFeed)
                .interceptors(Collections.singletonList(new OidcFeedOutInterceptor()));

        return new FeedConsumer<>(config);
    }
}
