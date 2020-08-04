package no.nav.fo.veilarbaktivitet.kafka;

import lombok.Builder;
import lombok.Value;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.domain.AktivitetStatus;
import no.nav.fo.veilarbaktivitet.domain.AktivitetTypeDTO;

import java.util.Date;

import static no.nav.fo.veilarbaktivitet.mappers.Helpers.typeMap;

@Value
@Builder
public class KafkaAktivitetMelding {
    String aktivitetId;
    String aktorId;
    Date fraDato;
    Date tilDato;
    Date endretDato;
    AktivitetTypeDTO aktivitetType;
    AktivitetStatus aktivitetStatus;
    boolean avtalt;
    boolean historisk;

    public static KafkaAktivitetMelding of(AktivitetData aktivitet) {
        return KafkaAktivitetMelding.builder()
                .aktorId(aktivitet.getAktorId())
                .aktivitetId(String.valueOf(aktivitet.getId()))
                .fraDato(aktivitet.getFraDato())
                .tilDato(aktivitet.getTilDato())
                .endretDato(aktivitet.getEndretDato())
                .aktivitetType(typeMap.get(aktivitet.getAktivitetType()))
                .aktivitetStatus(aktivitet.getStatus())
                .avtalt(aktivitet.isAvtalt())
                .historisk(aktivitet.getHistoriskDato() != null)
                .build();
    }
}
