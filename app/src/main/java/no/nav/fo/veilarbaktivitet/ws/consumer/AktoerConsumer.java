package no.nav.fo.veilarbaktivitet.ws.consumer;

import no.nav.tjeneste.virksomhet.aktoer.v2.Aktoer_v2PortType;
import no.nav.tjeneste.virksomhet.aktoer.v2.HentAktoerIdForIdentPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.HentIdentForAktoerIdPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentAktoerIdForIdentRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentIdentForAktoerIdRequest;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.slf4j.LoggerFactory.getLogger;

public class AktoerConsumer {

    private static final Logger LOG = getLogger(AktoerConsumer.class);

    @Inject
    private Aktoer_v2PortType aktoerV2;

    public Optional<String> hentAktoerIdForIdent(String ident) {
        if (isBlank(ident)) {
            LOG.warn("Kan ikke hente aktør-id uten fødselsnummer");
            return empty();
        }
        try {
            return of(aktoerV2.hentAktoerIdForIdent(
                    new WSHentAktoerIdForIdentRequest()
                            .withIdent(ident)
            ).getAktoerId());
        } catch (HentAktoerIdForIdentPersonIkkeFunnet e) {
            LOG.warn("AktoerID ikke funnet for fødselsnummer!", e);
            return empty();
        }
    }


    public Optional<String> hentIdentForAktørId(String aktørId) {
        if (isBlank(aktørId)) {
            LOG.warn("Kan ikke hente odemt uten aktørId");
            return empty();
        }

        try {
            return of(aktoerV2.hentIdentForAktoerId(
                    new WSHentIdentForAktoerIdRequest()
                            .withAktoerId(aktørId)
            ).getIdent());
        } catch (HentIdentForAktoerIdPersonIkkeFunnet e) {
            LOG.warn("AktoerID ikke funnet for aktørId!", e);
            return empty();
        }
    }

}
