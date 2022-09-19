package no.nav.veilarbaktivitet.aktivitetskort;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AktivitetskortService {

    private final AktivitetService aktivitetService;
    public void opprettTiltaksaktivitet(AktivitetskortDTO aktivitetskortDTO) {
//        aktivitetService.opprettAktivitet();
    }
}
