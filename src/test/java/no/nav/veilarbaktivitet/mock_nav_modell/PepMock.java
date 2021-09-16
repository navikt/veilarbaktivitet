package no.nav.veilarbaktivitet.mock_nav_modell;

import lombok.RequiredArgsConstructor;
import no.nav.common.abac.AbacClient;
import no.nav.common.abac.Pep;
import no.nav.common.abac.domain.request.ActionId;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import no.nav.common.types.identer.EksternBrukerId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.NavIdent;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PepMock implements Pep {

    private final AuthContextHolder authContextHolder;

    @Override
    public boolean harVeilederTilgangTilEnhet(NavIdent veilederIdent, EnhetId enhetId) {
        return MockNavService.getVeileder(veilederIdent.get()).harTilgangTilEnhet(enhetId.get());
    }

    @Override
    public boolean harTilgangTilEnhet(String innloggetBrukerIdToken, EnhetId enhetId) {
        if (authContextHolder.getRole().get().equals(UserRole.EKSTERN)) {
            return true;
        }
        String ident = authContextHolder.getSubject().get();
        return MockNavService.getVeileder(ident).harTilgangTilEnhet(enhetId.get());
    }

    @Override
    public boolean harTilgangTilEnhetMedSperre(String innloggetBrukerIdToken, EnhetId enhetId) {
        return harTilgangTilEnhet(innloggetBrukerIdToken, enhetId);
    }

    @Override
    public boolean harVeilederTilgangTilPerson(NavIdent veilederIdent, ActionId actionId, EksternBrukerId eksternBrukerId) {
        return this.harTilgangTilBruker(veilederIdent.get(), eksternBrukerId.get());
    }

    @Override
    public boolean harTilgangTilPerson(String innloggetBrukerIdToken, ActionId actionId, EksternBrukerId eksternBrukerId) {
        if (authContextHolder.getRole().get().equals(UserRole.EKSTERN)) {
            return MockNavService.getBruker(authContextHolder.getSubject().get()).harIdent(eksternBrukerId.get());
        }
        return this.harTilgangTilBruker(authContextHolder.getSubject().get(), eksternBrukerId.get());
    }

    private boolean harTilgangTilBruker(String veilederId, String brukerId) {
        MockBruker bruker = MockNavService.getBruker(brukerId);
        return MockNavService.getVeileder(veilederId).harTilgangTilBruker(bruker);
    }

    @Override
    public boolean harTilgangTilOppfolging(String innloggetBrukerIdToken) {
        return true;
    }

    @Override
    public boolean harVeilederTilgangTilModia(String innloggetVeilederIdToken) {
        return false;
    }

    @Override
    public boolean harVeilederTilgangTilKode6(NavIdent veilederIdent) {
        return false;
    }

    @Override
    public boolean harVeilederTilgangTilKode7(NavIdent veilederIdent) {
        return false;
    }

    @Override
    public boolean harVeilederTilgangTilEgenAnsatt(NavIdent veilederIdent) {
        return false;
    }

    @Override
    public AbacClient getAbacClient() {
        return null;
    }
}
