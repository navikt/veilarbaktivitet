package no.nav.fo.veilarbaktivitet.feed.consumer;

import no.nav.fo.veilarbaktivitet.db.dao.KvpDAO;
import no.nav.fo.veilarboppfolging.rest.domain.KvpDTO;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class KvpFeedConsumer {

    private static final Logger LOG = getLogger(KvpFeedConsumer.class);

    @Inject
    private KvpDAO repository;

    public String lastSerial() {
        return String.valueOf(repository.currentSerial());
    }

    public void consume(String lastEntryId, List<KvpDTO> elements) {
        LOG.info("Consuming {} new entries in KVP feed...", elements.size());
        elements.stream().forEach(k -> repository.insert(k));
    }
}
