package no.nav.veilarbaktivitet.stilling_fra_nav;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAktivitetIder;
import no.nav.veilarbaktivitet.brukernotifikasjon.kvittering.KvitteringDAO;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class BehandleNotifikasjonForDelingAvCvService {

    private final StillingFraNavProducerClient stillingFraNavProducerClient;
    private final AktivitetDAO aktivitetDAO;
    private final AktivitetService aktivitetService;
    private final KvitteringDAO kvitteringDAO;

    @Transactional
    public void behandleFerdigstiltKvittering(BrukernotifikasjonAktivitetIder brukernotifikasjon) {
        AktivitetData aktivitetData = aktivitetDAO.hentAktivitet(brukernotifikasjon.getAktivitetId());

        // Hvis aktiviteten er svart trenger vi ikke å varsle
        if (aktivitetData.getStillingFraNavData().cvKanDelesData != null) {
            kvitteringDAO.setFerdigBehandlet(brukernotifikasjon.getId());
            return;
        }

        var endretAv = Person.systemUser();

        AktivitetData nyAktivitet = aktivitetData.toBuilder()
                .endretAv(endretAv.get())
                .endretAvType(endretAv.tilInnsenderType())
                .endretDato(new Date())
                .stillingFraNavData(aktivitetData.getStillingFraNavData().withLivslopsStatus(LivslopsStatus.HAR_VARSLET)).build();
        aktivitetService.oppdaterAktivitet(aktivitetData, nyAktivitet);
        kvitteringDAO.setFerdigBehandlet(brukernotifikasjon.getId());

        stillingFraNavProducerClient.sendVarslet(aktivitetData);
    }

    public void behandleFeiletKvittering(BrukernotifikasjonAktivitetIder brukernotifikasjon) {
        AktivitetData aktivitetData = aktivitetDAO.hentAktivitet(brukernotifikasjon.getAktivitetId());

        // Hvis aktiviteten er svart trenger vi ikke å varsle men vi setter nofigikasjonen til ferdig behandlet
        if (aktivitetData.getStillingFraNavData().cvKanDelesData != null) {
            kvitteringDAO.setFerdigBehandlet(brukernotifikasjon.getId());
            return;
        }
        var endretAv = Person.systemUser();

        AktivitetData nyAktivitet = aktivitetData.toBuilder()
                .endretAv(endretAv.get())
                .endretAvType(endretAv.tilInnsenderType())
                .stillingFraNavData(aktivitetData.getStillingFraNavData().withLivslopsStatus(LivslopsStatus.KAN_IKKE_VARSLE)).build();
        aktivitetService.oppdaterAktivitet(aktivitetData, nyAktivitet);
        kvitteringDAO.setFerdigBehandlet(brukernotifikasjon.getId());

        stillingFraNavProducerClient.sendKanIkkeVarsle(aktivitetData);

    }
}
