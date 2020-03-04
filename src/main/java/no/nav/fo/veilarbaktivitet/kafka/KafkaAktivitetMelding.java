package no.nav.fo.veilarbaktivitet.kafka;

import lombok.Builder;
import lombok.Value;
import no.nav.common.utils.IdUtils;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.domain.AktivitetStatus;
import no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData;
import org.slf4j.MDC;

import java.util.Date;
import java.util.Optional;

import static no.nav.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;

@Value
@Builder
public class KafkaAktivitetMelding {
    String meldingId;
    Long aktivitetId;
    String aktorId;
    Date fraDato;
    Date tilDato;
    Date endretDato;
    AktivitetTypeData aktivitetType;
    AktivitetStatus aktivitetStatus;
    Boolean avtalt;
    Boolean historisk;

    public static KafkaAktivitetMelding of(AktivitetData aktivitet) {
        String correlationId = Optional.ofNullable(MDC.get(PREFERRED_NAV_CALL_ID_HEADER_NAME)).orElse(IdUtils.generateId());

        return KafkaAktivitetMelding.builder()
                .meldingId(correlationId)
                .aktorId(aktivitet.getAktorId())
                .aktivitetId(aktivitet.getId())
                .fraDato(aktivitet.getFraDato())
                .tilDato(aktivitet.getTilDato())
                .endretDato(aktivitet.getEndretDato())
                .aktivitetType(aktivitet.getAktivitetType())
                .aktivitetStatus(aktivitet.getStatus())
                .avtalt(aktivitet.isAvtalt())
                .build();
    }
}
