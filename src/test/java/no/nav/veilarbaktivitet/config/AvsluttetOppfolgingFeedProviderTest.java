package no.nav.veilarbaktivitet.config;

import no.nav.veilarbaktivitet.db.dao.AvsluttetOppfolgingFeedDAO;
import no.nav.veilarbaktivitet.domain.AvsluttetOppfolgingFeedDTO;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.feed.AvsluttetOppfolgingFeedConsumer;
import no.nav.veilarbaktivitet.service.AktivitetService;
import org.junit.Test;

import java.util.Date;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;

public class AvsluttetOppfolgingFeedProviderTest {

    private static final Person.AktorId AKTØR_ID = Person.aktorId("aktørId");

    private AktivitetService aktivitetService = mock(AktivitetService.class);
    private AvsluttetOppfolgingFeedDAO avsluttetOppfolgingFeedDAO = mock(AvsluttetOppfolgingFeedDAO.class);
    private AvsluttetOppfolgingFeedConsumer feedProvider = new AvsluttetOppfolgingFeedConsumer(aktivitetService, avsluttetOppfolgingFeedDAO);

    @Test
    public void skal_ta_vare_paa_siste_kjente_id() {

        Date oppdatert1 = new Date();
        Date oppdatert2 = new Date(oppdatert1.getTime() + 1000);
        feedProvider.lesAvsluttetOppfolgingFeed(null, asList(element(oppdatert1), element(oppdatert2)));
        verify(avsluttetOppfolgingFeedDAO).oppdaterSisteFeedId(oppdatert2);
    }


    @Test(expected=RuntimeException.class)
    public void skal_ikke_oppdatere_id_hvis_exception() {
        doThrow(new RuntimeException()).when(aktivitetService).settAktiviteterTilHistoriske(AKTØR_ID, null);
        
        try {
            feedProvider.lesAvsluttetOppfolgingFeed(null, asList(element(new Date())));
        } finally {
            verifyNoMoreInteractions(avsluttetOppfolgingFeedDAO);
        }
    }
    
    private AvsluttetOppfolgingFeedDTO element(Date oppdatert) {
        return new AvsluttetOppfolgingFeedDTO().setAktoerid(AKTØR_ID.get()).setOppdatert(oppdatert);
    }

}