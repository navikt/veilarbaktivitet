package no.nav.veilarbaktivitet.brukernotifikasjon.avlsutt;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Data
@RequiredArgsConstructor
class SkalAvluttes {
    private final String brukernotifikasjonId;
    private final String aktorId;
    private final UUID oppfolgingsPeriode;
}
