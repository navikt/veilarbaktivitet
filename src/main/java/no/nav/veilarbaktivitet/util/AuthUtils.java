package no.nav.veilarbaktivitet.util;

import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.text.ParseException;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

public class AuthUtils {

    private AuthUtils() {}

    public static boolean erSystemkallFraAzureAd(AuthContextHolder authContextHolder) {
        UserRole role = authContextHolder.getRole()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return UserRole.SYSTEM.equals(role) && harAADRolleForSystemTilSystemTilgang(authContextHolder);
    }

    private static boolean harAADRolleForSystemTilSystemTilgang(AuthContextHolder authContextHolder) {
        return authContextHolder.getIdTokenClaims()
                .flatMap(claims -> {
                    try {
                        return ofNullable(claims.getStringListClaim("roles"));
                    } catch (ParseException e) {
                        return empty();
                    }
                })
                .orElse(emptyList())
                .contains("access_as_application");
    }
}