package no.nav.veilarbaktivitet.aktivitetskort.dto.kassering;

import lombok.Getter;
import no.nav.common.types.identer.NavIdent;
import no.nav.common.types.identer.NorskIdent;
import no.nav.veilarbaktivitet.aktivitetskort.ActionType;
import no.nav.veilarbaktivitet.aktivitetskort.dto.BestillingBase;

import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
public class KasseringsBestilling extends BestillingBase {
    private final NavIdent navIdent;
    private final NorskIdent personIdent;
    private final UUID aktivitetsId;
    private final String begrunnelse;

    public KasseringsBestilling(String source, UUID messageId, ActionType actionType, NavIdent navIdent, NorskIdent personIdent, UUID aktivitetsId, String begrunnelse) {
        super(source, messageId, actionType);
        this.navIdent = navIdent;
        this.personIdent = personIdent;
        this.aktivitetsId = aktivitetsId;
        this.begrunnelse = begrunnelse;
    }

    @Override
    public UUID getAktivitetskortId() {
        return this.aktivitetsId;
    }
}
