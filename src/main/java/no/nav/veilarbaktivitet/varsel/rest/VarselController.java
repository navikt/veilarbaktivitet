package no.nav.veilarbaktivitet.varsel.rest;

import lombok.AllArgsConstructor;
import no.nav.veilarbaktivitet.varsel.VarselService;
import no.nav.veilarbaktivitet.varsel.event.VarselEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/varsel")
public class VarselController {

    private final VarselService service;

    /**
     * Test endepunkt for å sende et varsel, skal slettes før systemet går ut i produksjon.
     * Det er ingen produsenter som bruker dette endepunktet
     *
     * @deprecated Skal slettes når varsel er ferdig testet
     */
    @Deprecated(forRemoval = true)
    @PostMapping
    public ResponseEntity<VarselEvent> sendVarsel() {
        final VarselEvent varsel = service.sendVarsler();
        return ResponseEntity.ok(varsel);
    }

    /**
     * Test endepunkt for å sende et varsel, skal slettes før systemet går ut i produksjon.
     * Det er ingen produsenter som bruker dette endepunktet
     *
     * @deprecated Skal slettes når varsel er ferdig testet
     */
    @Deprecated(forRemoval = true)
    @PostMapping("/done")
    public ResponseEntity<VarselEvent> sendDone(@RequestParam String id) {
        final VarselEvent done = service.sendStopp(id);
        return ResponseEntity.ok(done);
    }
}
