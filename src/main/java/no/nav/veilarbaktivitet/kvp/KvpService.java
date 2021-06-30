package no.nav.veilarbaktivitet.kvp;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.domain.KvpDTO;
import no.nav.veilarbaktivitet.domain.Person;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.InternalServerErrorException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KvpService {

    private final KvpClient kvpClient;

    public boolean erUnderKvp(Person.AktorId aktorId) {
        Optional<KvpDTO> kvpDTO = kvpClient.get(aktorId);

        if(kvpDTO.isEmpty()) {
            return false;
        }

        return kvpDTO
                .map(it -> it.getKvpId() != 0L && it.getAvsluttetDato() != null )
                .orElse(false);
    }


    public AktivitetData tagUsingKVP(AktivitetData a) {
        try {
            Optional<KvpDTO> kvp = kvpClient.get(Person.aktorId(a.getAktorId()));
            return kvp
                    .map(k -> a.toBuilder().kontorsperreEnhetId(k.getEnhet()).build())
                    .orElse(a);
        } catch (ForbiddenException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "veilarbaktivitet har ikke tilgang til å spørre om KVP-status.");
        } catch (InternalServerErrorException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "veilarboppfolging har en intern bug, vennligst fiks applikasjonen.");
        }
    }
}
