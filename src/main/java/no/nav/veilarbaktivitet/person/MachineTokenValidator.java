package no.nav.veilarbaktivitet.person;

import com.nimbusds.jwt.JWTClaimsSet;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.context.AuthContextHolder;

import java.text.ParseException;
import java.util.List;
import java.util.Optional;

@Slf4j
public class MachineTokenValidator {
    private static final String DIRIGENT_ROLE = "service-opprett-aktivitet";
    public static boolean sjekkHarSystemRolle(AuthContextHolder authContextHolder) {
        try {
            var claims = authContextHolder.getIdTokenClaims();
            if (!isAzureMachineToken(claims)) return false;
            List<String> roles = claims.get().getStringListClaim("roles");
            if (roles == null) return false;
            return roles.stream().anyMatch(role -> role.equals(DIRIGENT_ROLE));
        } catch (ParseException e) {
            log.warn("Kunne ikke hente IdTokenClaims");
            return false;
        }
    }

    private static boolean isAzureMachineToken(Optional<JWTClaimsSet> claims) throws ParseException {
        if (claims.isEmpty()) return false;
        return claims.get().getSubject().equals(claims.get().getStringClaim("oid"));
    }
}
