package no.nav.veilarbaktivitet.motesms;

import no.nav.veilarbaktivitet.aktivitet.dto.KanalDTO;
import no.nav.veilarbaktivitet.person.Person;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

record MoteNotifikasjon(long aktivitetId, long aktitetVersion, Person.AktorId aktorId, KanalDTO kanalDTO,
                        ZonedDateTime startTid) {
    private static final String melding = "Vi minner om at du har et %s %s";
    private static final String EPOST_TITEL = "Påminnelse om møte";

    String getMoteTid() {
        return startTid.format(DateTimeFormatter.ofPattern("EEEE d. MMMM 'kl.' HH:mm", Locale.forLanguageTag("no")));
    }

    private String getTekst() {
        return melding.formatted( kanalDTO.getTekst(), getMoteTid());
    }

    String getSmsTekst() {
        return getTekst();
    }

    String getEpostTitel() {
        return EPOST_TITEL;
    }

    String getEpostBody() {
        return getTekst() + " \nVennlig hilsen NAV";
    }

    String getDitNavTekst() {
        return getTekst();
    }

}
