package no.nav.veilarbaktivitet.feed;


import no.nav.veilarbaktivitet.db.dao.AvsluttetOppfolgingFeedDAO;
import no.nav.veilarbaktivitet.domain.AvsluttetOppfolgingFeedDTO;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.service.AktivitetService;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import static java.util.Optional.ofNullable;

@Component
public class AvsluttetOppfolgingFeedConsumer {

    private AktivitetService aktivitetService;

    private AvsluttetOppfolgingFeedDAO avsluttetOppfolgingFeedDAO;

    public AvsluttetOppfolgingFeedConsumer(AktivitetService aktivitetService, AvsluttetOppfolgingFeedDAO avsluttetOppfolgingFeedDAO) {
        this.aktivitetService = aktivitetService;
        this.avsluttetOppfolgingFeedDAO = avsluttetOppfolgingFeedDAO;
    }

    public String sisteKjenteId() {
        ZonedDateTime dateEpoch = new Date(0).toInstant().atZone(ZoneId.systemDefault());
        ZonedDateTime sisteKjenteId = ofNullable(avsluttetOppfolgingFeedDAO.hentSisteKjenteId()).orElseGet(()->dateEpoch);
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
