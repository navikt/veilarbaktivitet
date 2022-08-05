package no.nav.veilarbaktivitet.stilling_fra_nav;

import java.util.Date;

public record RekrutteringsbistandStatusoppdatering(RekrutteringsbistandStatusoppdateringEventType type, String detaljer, String utførtAvNavIdent, Date tidspunkt) {
}