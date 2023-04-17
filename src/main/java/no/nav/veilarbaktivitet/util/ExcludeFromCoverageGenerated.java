package no.nav.veilarbaktivitet.util;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Retention(RUNTIME)
@Target({TYPE, METHOD, CONSTRUCTOR})
/*
  Jacoco vil ignorere klasser og metoder som er annotert med en annotasjon som inneholder 'Generated' i navnet.
 */
public @interface ExcludeFromCoverageGenerated {
}
