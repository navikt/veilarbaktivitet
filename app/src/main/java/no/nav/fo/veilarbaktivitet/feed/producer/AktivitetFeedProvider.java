package no.nav.fo.veilarbaktivitet.feed.producer;

import no.nav.fo.feed.common.FeedElement;
import no.nav.fo.feed.producer.FeedProvider;
import no.nav.fo.veilarbaktivitet.db.dao.AktivitetDAO;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.ZoneId;
import java.util.stream.Stream;

import static no.nav.fo.veilarbaktivitet.util.DateUtils.*;


@Component
public class AktivitetFeedProvider implements FeedProvider<AktivitetFeedData>{

    private AktivitetDAO aktivitetDAO;

    @Inject
    public AktivitetFeedProvider(AktivitetDAO aktivitetDAO) {
        this.aktivitetDAO = aktivitetDAO;
    }

    @Override
    public Stream<FeedElement<AktivitetFeedData>> fetchData(String sinceId, int i) {

        return aktivitetDAO
                .hentAktiviteterEtterTidspunkt(dateFromISO8601(sinceId))
                .stream()
                .map(a -> new FeedElement<AktivitetFeedData>()
                        .setId(ISO8601FromDate(a.getOpprettetDato(), ZoneId.systemDefault()))
                        .setElement(a));
    }
}
