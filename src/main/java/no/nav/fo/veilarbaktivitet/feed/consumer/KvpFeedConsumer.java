package no.nav.fo.veilarbaktivitet.feed.consumer;

import no.nav.fo.veilarbaktivitet.db.dao.KVPFeedDAO;
import no.nav.fo.veilarbaktivitet.domain.Person;
import no.nav.fo.veilarbaktivitet.service.AktivitetService;
import no.nav.fo.veilarboppfolging.rest.domain.KvpDTO;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

@Component
public class KvpFeedConsumer {

    private AktivitetService aktivitetService;

    private KVPFeedDAO kvpFeedDAO;

    @Inject
    public KvpFeedConsumer(AktivitetService aktivitetService, KVPFeedDAO kvpFeedDAO) {
        this.aktivitetService = aktivitetService;
        this.kvpFeedDAO = kvpFeedDAO;
    }

    public String sisteKjenteKvpId() {
        return String.valueOf(kvpFeedDAO.hentSisteKVPFeedId());
    }

    public void lesKvpFeed(String lastEntryId, List<KvpDTO> elements) {
        elements.stream()
                .map(element -> {
                    if(element.getAvsluttetDato()!= null) {
                        aktivitetService.settAktiviteterInomKVPPeriodeTilHistoriske(Person.aktorId(element.getAktorId()), element.getAvsluttetDato());
                    }
                    return element.getSerial();
                })
                .reduce((a1, a2) -> a2)
                .ifPresent(lastSuccessfulId -> kvpFeedDAO.oppdaterSisteKVPFeedId(lastSuccessfulId));
    }
}
