package no.nav.fo.veilarbaktivitet.kafka;

import lombok.Builder;
import lombok.Value;
import no.nav.common.utils.IdUtils;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.domain.AktivitetStatus;
import no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData;

import java.time.Instant;

@Value
@Builder
public class KafkaAktivitetMelding {
    String meldingId;
    Long aktivitetId;
    String aktorId;
    Instant fraDato;
    Instant tilDato;
    Instant endretDato;
    AktivitetTypeData aktivitetType;
    AktivitetStatus aktivitetStatus;
    Boolean avtalt;
    Boolean historisk;

    public static KafkaAktivitetMelding of(AktivitetData aktivitet) {
        return KafkaAktivitetMelding.builder()
                .meldingId(IdUtils.generateId())
                .aktorId(aktivitet.getAktorId())
                .aktivitetId(aktivitet.getId())
                .fraDato(aktivitet.getFraDato().toInstant())
                .tilDato(aktivitet.getTilDato().toInstant())
                .endretDato(aktivitet.getEndretDato().toInstant())
                .aktivitetType(aktivitet.getAktivitetType())
                .aktivitetStatus(aktivitet.getStatus())
                .avtalt(aktivitet.isAvtalt())
                .build();
    }
}
