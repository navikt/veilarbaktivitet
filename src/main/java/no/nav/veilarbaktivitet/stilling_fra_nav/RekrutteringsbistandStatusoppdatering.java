package no.nav.veilarbaktivitet.stilling_fra_nav;

import java.time.ZonedDateTime;

public record RekrutteringsbistandStatusoppdatering(
        RekrutteringsbistandStatusoppdateringEventType type,
        String detaljer,
        String utførtAvNavIdent,
        ZonedDateTime tidspunkt
) {
}