package no.nav.veilarbaktivitet.kvp;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.InternalServerErrorException;
import lombok.RequiredArgsConstructor;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.EnhetId;
import no.nav.veilarbaktivitet.kvp.v2.KontorSperre;
import no.nav.veilarbaktivitet.kvp.v2.KvpV2Client;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KvpService {

    private final KvpV2Client kvpClient;
    private final AktorOppslagClient aktorOppslagClient;

    public boolean erUnderKvp(Person.AktorId aktorId) {
        var fnr = aktorOppslagClient.hentFnr(aktorId.otherAktorId());
        var kvpDTO = kvpClient.get(Person.fnr(fnr.get()));
        return kvpDTO.isPresent();
    }

    public Optional<EnhetId> getKontorSperreEnhet(Person.AktorId aktorId) {
        var fnr = aktorOppslagClient.hentFnr(aktorId.otherAktorId());
        try {
            Optional<KontorSperre> kvp = kvpClient.get(Person.fnr(fnr.get()));
            return kvp.map(KontorSperre::getEnhetId);
        } catch (ForbiddenException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "veilarbaktivitet har ikke tilgang til å spørre om KVP-status.");
        } catch (InternalServerErrorException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "veilarboppfolging har en intern bug, vennligst fiks applikasjonen.");
        }
    }
}
