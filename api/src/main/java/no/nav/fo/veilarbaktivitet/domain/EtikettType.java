package no.nav.fo.veilarbaktivitet.domain;


import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Etikett;

import java.util.HashMap;
import java.util.Map;

import static no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Etikett.*;

@Data
@Accessors(chain = true)
public class EtikettType {

    public String id;
    public String type;
    public String visningsTekst;

    private static final String OK = "ok";
    private static final String VARSLING = "varsling";
    private static final String INFO = "info";

    private static final Map<Etikett, EtikettType> etikettTyper = new HashMap<>();
    private static final Map<EtikettType, Etikett> etiketter = new HashMap<>();

    static {
        definer(AVSLAG, "Avslag", VARSLING);
        definer(SOEKNAD_SENDT, "Søknad sendt", OK);
        definer(INNKALDT_TIL_INTERVJU, "Innkalt til intervju", INFO);
        definer(JOBBTILBUD, "Jobbtilbud", OK);
    }

    private static void definer(Etikett etikett, String visningsTekst, String type) {
        EtikettType etikettType = new EtikettType()
                .setId(etikett.name())
                .setVisningsTekst(visningsTekst)
                .setType(type);
        etikettTyper.put(etikett, etikettType);
        etiketter.put(etikettType, etikett);
    }

    public static EtikettType fraEtikett(Etikett etikett) {
        return etikettTyper.get(etikett);
    }

    public static EtikettType valueOf(String id) {
        return etikettTyper.values().stream()
                .filter(etikettType -> etikettType.id.equals(id))
                .findAny()
                .orElse(null);
    }

    public Etikett getEtikett() {
        return etiketter.get(this);
    }

}
