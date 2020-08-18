package no.nav.veilarbaktivitet.feed;


import no.nav.veilarbaktivitet.db.dao.KVPFeedDAO;
import no.nav.veilarbaktivitet.domain.KvpDTO;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.service.AktivitetService;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class KvpFeedConsumer {

    private AktivitetService aktivitetService;

    private KVPFeedDAO kvpFeedDAO;

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
                        aktivitetService.settAktiviteterInomKVPPeriodeTilAvbrutt(Person.aktorId(element.getAktorId()), element.getAvsluttetBegrunnelse(), element.getAvsluttetDato());
                    }
                    return element.getSerial();
                })
                .reduce((a1, a2) -> a2)
                .ifPresent(lastSuccessfulId -> kvpFeedDAO.oppdaterSisteKVPFeedId(lastSuccessfulId));
    }
}