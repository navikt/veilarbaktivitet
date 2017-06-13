package no.nav.fo.veilarbaktivitet.feed.producer;

import no.nav.fo.feed.common.FeedElement;
import no.nav.fo.feed.producer.FeedProvider;
import no.nav.fo.veilarbaktivitet.db.dao.AktivitetFeedDAO;
import no.nav.fo.veilarbaktivitet.domain.AktivitetFeedData;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.ZoneId;
import java.util.stream.Stream;

import static no.nav.fo.veilarbaktivitet.util.DateUtils.ISO8601FromDate;
import static no.nav.fo.veilarbaktivitet.util.DateUtils.dateFromISO8601;


@Component
public class AktivitetFeedProvider implements FeedProvider<AktivitetFeedData> {

    private AktivitetFeedDAO aktivitetFeedDAO;

    @Inject
    public AktivitetFeedProvider(AktivitetFeedDAO aktivitetFeedDAO) {
        this.aktivitetFeedDAO = aktivitetFeedDAO;
    }

    @Override
    public Stream<FeedElement<AktivitetFeedData>> fetchData(String sinceId, int i) {
        return aktivitetFeedDAO
                .hentAktiviteterEtterTidspunkt(dateFromISO8601(sinceId))
                .stream()
                .map(a -> new FeedElement<AktivitetFeedData>()
                        .setId(ISO8601FromDate(a.getEndretDato(), ZoneId.systemDefault()))
                        .setElement(a));
    }
}
