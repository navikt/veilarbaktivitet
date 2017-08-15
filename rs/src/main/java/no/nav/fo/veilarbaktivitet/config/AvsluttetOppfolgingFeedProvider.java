package no.nav.fo.veilarbaktivitet.config;

import no.nav.fo.veilarbaktivitet.db.dao.AktivitetFeedDAO;
import no.nav.fo.veilarbaktivitet.service.AktivitetService;
import no.nav.fo.veilarbsituasjon.rest.domain.AvsluttetOppfolgingFeedDTO;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import static java.util.Optional.ofNullable;

@Component
public class AvsluttetOppfolgingFeedProvider {

    @Inject
    AktivitetService aktivitetService;

    @Inject
    AktivitetFeedDAO aktivitetFeedDAO;

    public String sisteEndring() {
        Date sisteEndring = ofNullable(aktivitetFeedDAO.hentSisteHistoriskeTidspunkt()).orElseGet(() -> new Date(0));
        return ZonedDateTime.ofInstant(sisteEndring.toInstant(), ZoneId.systemDefault()).toString();
    }

    public void lesAvsluttetOppfolgingFeed(String lastEntryId, List<AvsluttetOppfolgingFeedDTO> elements) {
        elements.forEach(element -> aktivitetService.settAktiviteterTilHistoriske(element));
    }
}
