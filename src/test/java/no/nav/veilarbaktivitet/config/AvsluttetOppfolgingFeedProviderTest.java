package no.nav.veilarbaktivitet.config;

import no.nav.veilarbaktivitet.db.dao.AvsluttetOppfolgingFeedDAO;
import no.nav.veilarbaktivitet.domain.AvsluttetOppfolgingFeedDTO;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.feed.AvsluttetOppfolgingFeedConsumer;
import no.nav.veilarbaktivitet.service.AktivitetService;
import org.junit.Test;

import java.time.ZonedDateTime;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;

public class AvsluttetOppfolgingFeedProviderTest {

    private static final Person.AktorId AKTØR_ID = Person.aktorId("aktørId");

    private AktivitetService aktivitetService = mock(AktivitetService.class);
    private AvsluttetOppfolgingFeedDAO avsluttetOppfolgingFeedDAO = mock(AvsluttetOppfolgingFeedDAO.class);
    private AvsluttetOppfolgingFeedConsumer feedProvider = new AvsluttetOppfolgingFeedConsumer(aktivitetService, avsluttetOppfolgingFeedDAO);

    @Test
    public void skal_ta_vare_paa_siste_kjente_id() {

        ZonedDateTime oppdatert1 = ZonedDateTime.now();
        ZonedDateTime oppdatert2 = oppdatert1.plusHours(5);
        feedProvider.lesAvsluttetOppfolgingFeed(null, asList(element(oppdatert1), element(oppdatert2)));
        verify(avsluttetOppfolgingFeedDAO).oppdaterSisteFeedId(oppdatert2);
    }


    @Test(expected=RuntimeException.class)
    public void skal_ikke_oppdatere_id_hvis_exception() {
        doThrow(new RuntimeException()).when(aktivitetService).settAktiviteterTilHistoriske(AKTØR_ID, null);
        
        try {
            feedProvider.lesAvsluttetOppfolgingFeed(null, asList(element(ZonedDateTime.now())));
        } finally {
            verifyNoMoreInteractions(avsluttetOppfolgingFeedDAO);
        }
    }
    
    private AvsluttetOppfolgingFeedDTO element(ZonedDateTime oppdatert) {
        return new AvsluttetOppfolgingFeedDTO().setAktoerid(AKTØR_ID.get()).setOppdatert(oppdatert);
    }

}