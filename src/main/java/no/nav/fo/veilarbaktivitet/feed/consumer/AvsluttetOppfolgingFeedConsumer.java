package no.nav.fo.veilarbaktivitet.feed.consumer;

import no.nav.fo.veilarbaktivitet.db.dao.AvsluttetOppfolgingFeedDAO;
import no.nav.fo.veilarbaktivitet.domain.AvsluttetOppfolgingFeedDTO;
import no.nav.fo.veilarbaktivitet.domain.Person;
import no.nav.fo.veilarbaktivitet.service.AktivitetService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import static java.util.Optional.ofNullable;

@Component
public class AvsluttetOppfolgingFeedConsumer {

    private AktivitetService aktivitetService;

    private AvsluttetOppfolgingFeedDAO avsluttetOppfolgingFeedDAO;

    @Inject
    public AvsluttetOppfolgingFeedConsumer(AktivitetService aktivitetService, AvsluttetOppfolgingFeedDAO avsluttetOppfolgingFeedDAO) {
        this.aktivitetService = aktivitetService;
        this.avsluttetOppfolgingFeedDAO = avsluttetOppfolgingFeedDAO;
    }

    public String sisteKjenteId() {
        Date sisteKjenteId = ofNullable(avsluttetOppfolgingFeedDAO.hentSisteKjenteId()).orElseGet(() -> new Date(0));
        return ZonedDateTime.ofInstant(sisteKjenteId.toInstant(), ZoneId.systemDefault()).toString();
    }

    public void lesAvsluttetOppfolgingFeed(String lastEntryId, List<AvsluttetOppfolgingFeedDTO> elements) {

        // Håndterer ikke exceptions her. Dersom en exception oppstår i løkkeprosesseringen over, vil
        // vi altså IKKE få oppdatert siste id. Dermed vil vi lese feeden på nytt fra siste kjente id og potensielt
        // prosessere noen elementer flere ganger. Dette skal gå bra, siden koden som setter dialoger til historisk
        // er idempotent

        elements.stream()
                .map(element -> {
                    aktivitetService.settAktiviteterTilHistoriske(Person.aktorId(element.getAktoerid()), element.getSluttdato());
                    return element.getOppdatert();
                })
                .reduce((a1, a2) -> a2)
                .ifPresent(lastSuccessfulId -> avsluttetOppfolgingFeedDAO.oppdaterSisteFeedId(lastSuccessfulId));
    }
}
