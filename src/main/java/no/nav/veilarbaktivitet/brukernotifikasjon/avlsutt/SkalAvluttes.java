package no.nav.veilarbaktivitet.brukernotifikasjon.avlsutt;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
class SkalAvluttes {
    private final String brukernotifikasjonId;
    private final String aktorId;
}
