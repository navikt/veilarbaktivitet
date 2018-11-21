package no.nav.fo.veilarbaktivitet.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider;
import no.nav.brukerdialog.security.oidc.OidcFeedOutInterceptor;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.consumer.FeedConsumerConfig;
import no.nav.fo.feed.consumer.FeedConsumerConfig.BaseConfig;
import no.nav.fo.feed.consumer.FeedConsumerConfig.SimplePollingConfig;
import no.nav.fo.veilarbaktivitet.feed.consumer.AvsluttetOppfolgingFeedConsumer;
import no.nav.fo.veilarboppfolging.rest.domain.AvsluttetOppfolgingFeedDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.Collections;

@Configuration
public class AvsluttetOppfolgingFeedConfig {

    private static final int LOCK_HOLDING_LIMIT_IN_MS = 10 * 60 * 1000;

    @Value("${veilarboppfolging.api.url}")
    private String host;

    @Value("${avsluttoppfolging.feed.pollingintervalseconds: 10}")
    private int pollingIntervalInSeconds;

    @Inject
    private DataSource dataSource;

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcLockProvider(dataSource);
    }

    @Bean
    public FeedConsumer<AvsluttetOppfolgingFeedDTO> avsluttetOppfolgingFeedItemFeedConsumer(AvsluttetOppfolgingFeedConsumer avsluttetOppfolgingFeedConsumer) {
        BaseConfig<AvsluttetOppfolgingFeedDTO> baseConfig = new BaseConfig<>(
                AvsluttetOppfolgingFeedDTO.class,
                avsluttetOppfolgingFeedConsumer::sisteKjenteId,
                host,
                AvsluttetOppfolgingFeedDTO.FEED_NAME
        );

        FeedConsumerConfig<AvsluttetOppfolgingFeedDTO> config = new FeedConsumerConfig<>(baseConfig, new SimplePollingConfig(pollingIntervalInSeconds))
                .lockProvider(lockProvider(dataSource), LOCK_HOLDING_LIMIT_IN_MS)
                .callback(avsluttetOppfolgingFeedConsumer::lesAvsluttetOppfolgingFeed)
                .interceptors(Collections.singletonList(new OidcFeedOutInterceptor()));

        return new FeedConsumer<>(config);
    }
}
