package no.nav.veilarbaktivitet.aktivitetskort.dto.kassering;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import no.nav.common.types.identer.NavIdent;
import no.nav.common.types.identer.NorskIdent;
import no.nav.veilarbaktivitet.aktivitetskort.ActionType;
import no.nav.veilarbaktivitet.aktivitetskort.dto.BestillingBase;

import java.util.UUID;

@SuperBuilder(toBuilder = true)
@Getter
@NoArgsConstructor
public class KasseringsBestilling extends BestillingBase {
    private NavIdent navIdent;
    private NorskIdent personIdent;
    private UUID aktivitetsId;
    private String begrunnelse;

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
