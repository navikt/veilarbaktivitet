package no.nav.fo.veilarbaktivitet.feed.producer;

import no.nav.fo.feed.common.FeedElement;
import no.nav.fo.feed.producer.FeedProvider;
import no.nav.fo.veilarbaktivitet.db.dao.AktivitetDAO;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.*;
import java.util.stream.Stream;


@Component
public class AktivitetFeedProvider implements FeedProvider<AktivitetFeedData>{

    private AktivitetDAO aktivitetDAO;

    @Inject
    public AktivitetFeedProvider(AktivitetDAO aktivitetDAO) {
        this.aktivitetDAO = aktivitetDAO;
    }

    @Override
    public Stream<FeedElement<AktivitetFeedData>> fetchData(String sinceId, int i) {
        ZonedDateTime zoned = ZonedDateTime.parse(sinceId);
        Instant instant = Instant.from(zoned);
        LocalDateTime localTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        Timestamp timestamp = Timestamp.valueOf(localTime);

        return aktivitetDAO
                .hentAktiviteterEtterTidspunkt(timestamp)
                .stream()
                .map(a -> new FeedElement<AktivitetFeedData>()
                        .setId(toZonedDateTime(a.getOpprettetDato()).toString())
                        .setElement(a));
    }

    private ZonedDateTime toZonedDateTime(Timestamp endretTimestamp) {
        LocalDateTime localDateTime = endretTimestamp.toLocalDateTime();
        return ZonedDateTime.of(localDateTime, ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC);
    }
}
