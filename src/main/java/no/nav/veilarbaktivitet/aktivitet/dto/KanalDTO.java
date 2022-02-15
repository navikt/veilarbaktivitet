package no.nav.veilarbaktivitet.aktivitet.dto;

public enum KanalDTO {
    OPPMOTE,
    TELEFON,
    INTERNETT;

    public String getTekst() {
        return switch (this) {
            case OPPMOTE -> "møte";
            case TELEFON -> "telefonmøte";
            case INTERNETT -> "videomøte";
        };
    }
}
