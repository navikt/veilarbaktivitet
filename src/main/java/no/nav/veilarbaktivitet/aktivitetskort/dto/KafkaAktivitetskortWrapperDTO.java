package no.nav.veilarbaktivitet.aktivitetskort.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import no.nav.veilarbaktivitet.aktivitetskort.ActionType;
import no.nav.veilarbaktivitet.aktivitetskort.Aktivitetskort;
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortType;

import java.util.UUID;

@SuperBuilder(toBuilder = true)
@Getter
@NoArgsConstructor
public class KafkaAktivitetskortWrapperDTO extends BestillingBase {
    @JsonProperty(required = true)
    AktivitetskortType aktivitetskortType;
    @JsonProperty(required = true)
    Aktivitetskort aktivitetskort;

    @Override
    public UUID getAktivitetskortId() {
        return aktivitetskort.getId();
    }
}
