package no.nav.veilarbaktivitet.kvp;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.InternalServerErrorException;
import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.kvp.v2.KvpV2Client;
import no.nav.veilarbaktivitet.kvp.v2.KvpV2DTO;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KvpService {

    private final KvpV2Client kvpClient;

    public boolean erUnderKvp(Person.AktorId aktorId) {
        Optional<KvpV2DTO> kvpDTO = kvpClient.get(aktorId);

        if (kvpDTO.isEmpty()) {
            return false;
        }

        return kvpDTO
                .map(it -> it.getEnhet() != null && !it.getEnhet().isEmpty() && it.getAvsluttetDato() == null)
                .orElse(false);
    }


    public Optional<String> getKontorSperreEnhet(Person.AktorId aktorId) {
        try {
            Optional<KvpV2DTO> kvp = kvpClient.get(aktorId);
            return kvp.map(k -> k.getEnhet());
        } catch (ForbiddenException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "veilarbaktivitet har ikke tilgang til å spørre om KVP-status.");
        } catch (InternalServerErrorException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "veilarboppfolging har en intern bug, vennligst fiks applikasjonen.");
        }
    }
}
