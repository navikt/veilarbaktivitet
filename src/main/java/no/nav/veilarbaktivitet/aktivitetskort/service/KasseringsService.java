package no.nav.veilarbaktivitet.aktivitetskort.service;


import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.KasseringDAO;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.KasseringsBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.feil.AktivitetIkkeFunnetFeil;
import no.nav.veilarbaktivitet.aktivitetskort.feil.ErrorMessage;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KasseringsService {
    private final AktivitetDAO aktivitetDAO;
    private final AktivitetService aktivitetService;
    private final KasseringDAO kasseringDAO;


    public void kassertAktivitet(KasseringsBestilling kasseringsBestilling) throws AktivitetIkkeFunnetFeil {
        var aktivitet = aktivitetDAO.hentAktivitetByFunksjonellId(kasseringsBestilling.getAktivitetsId());
        if (aktivitet.isEmpty()) throw new AktivitetIkkeFunnetFeil(new ErrorMessage("Kan ikke kassere aktivitet som ikke finnes"), null);
        else {
            var navIdent = Person.navIdent(kasseringsBestilling.getNavIdent().get());
            kasserAktivitetMedTekniskId(aktivitet.get(), navIdent, kasseringsBestilling.getBegrunnelse());
        }
    }

    private void kasserAktivitetMedTekniskId(AktivitetData aktivitet, Person.NavIdent navIdent, String begrunnelse) {
        if (aktivitet.getStatus() != AktivitetStatus.AVBRUTT && aktivitet.getStatus() != AktivitetStatus.FULLFORT) {
            aktivitetService.oppdaterStatus(
                    aktivitet,
                    aktivitet.withStatus(AktivitetStatus.AVBRUTT),
                    navIdent.tilIdent()
            );
        }
        kasseringDAO.kasserAktivitetMedBegrunnelse(aktivitet.getId(), begrunnelse);
    }
}
