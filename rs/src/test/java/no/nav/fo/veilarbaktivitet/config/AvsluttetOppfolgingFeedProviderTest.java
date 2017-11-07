package no.nav.fo.veilarbaktivitet.config;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Date;

import org.junit.Test;

import no.nav.fo.veilarbaktivitet.db.dao.AvsluttetOppfolgingFeedDAO;
import no.nav.fo.veilarbaktivitet.service.AktivitetService;
import no.nav.fo.veilarboppfolging.rest.domain.AvsluttetOppfolgingFeedDTO;

public class AvsluttetOppfolgingFeedProviderTest {

    private static final String AKTØR_ID = "aktørId";

    private AktivitetService aktivitetService = mock(AktivitetService.class);
    private AvsluttetOppfolgingFeedDAO avsluttetOppfolgingFeedDAO = mock(AvsluttetOppfolgingFeedDAO.class);    
    private AvsluttetOppfolgingFeedProvider feedProvider = new AvsluttetOppfolgingFeedProvider(aktivitetService, avsluttetOppfolgingFeedDAO); 

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
        return new AvsluttetOppfolgingFeedDTO().setAktoerid(AKTØR_ID).setOppdatert(oppdatert);
    }

}