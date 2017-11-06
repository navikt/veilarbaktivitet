package no.nav.fo.veilarbaktivitet.config;

import no.nav.fo.veilarbaktivitet.db.dao.AktivitetFeedDAO;
import no.nav.fo.veilarbaktivitet.service.AktivitetService;
import no.nav.fo.veilarboppfolging.rest.domain.AvsluttetOppfolgingFeedDTO;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import static java.util.Optional.ofNullable;

@Component
public class AvsluttetOppfolgingFeedProvider {

    private AktivitetService aktivitetService;

    private AktivitetFeedDAO aktivitetFeedDAO;

    @Inject
    public AvsluttetOppfolgingFeedProvider(AktivitetService aktivitetService, AktivitetFeedDAO aktivitetFeedDAO) {
        this.aktivitetService = aktivitetService;
        this.aktivitetFeedDAO = aktivitetFeedDAO;
    }

    public String sisteKjenteId() {
        Date sisteKjenteId = ofNullable(aktivitetFeedDAO.hentSisteKjenteId()).orElseGet(() -> new Date(0));
        return ZonedDateTime.ofInstant(sisteKjenteId.toInstant(), ZoneId.systemDefault()).toString();
    }

    public void lesAvsluttetOppfolgingFeed(String lastEntryId, List<AvsluttetOppfolgingFeedDTO> elements) {
        Date lastSuccessfulId = null;
        for (AvsluttetOppfolgingFeedDTO element : elements) {
            aktivitetService.settAktiviteterTilHistoriske(element.getAktoerid(), element.getSluttdato());
            lastSuccessfulId = element.getOppdatert();
        }

        // Håndterer ikke exceptions her. Dersom en exception oppstår i løkkeprosesseringen over, vil 
        // vi altså IKKE få oppdatert siste id. Dermed vil vi lese feeden på nytt fra siste kjente id og potensielt
        // prosessere noen elementer flere ganger. Dette skal gå bra, siden koden som setter dialoger til historisk
        // er idempotent
        if(lastSuccessfulId != null) {
            aktivitetFeedDAO.oppdaterSisteFeedId(lastSuccessfulId);
        }        
    }
}
