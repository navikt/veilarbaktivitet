package no.nav.veilarbaktivitet.aktiviterTilKafka;

import lombok.Builder;
import lombok.Value;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.domain.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.domain.InnsenderData;

import java.util.Date;

import static no.nav.veilarbaktivitet.mappers.Helpers.typeMap;

@Value
@Builder
public class KafkaAktivitetMeldingV3 {
    String aktivitetId;
    Long version;
    String aktorId;
    Date fraDato;
    Date tilDato;
    Date endretDato;
    AktivitetTypeDTO aktivitetType;
    AktivitetStatus aktivitetStatus;
    SisteEndringKategori sisteEndringKategori;
    InnsenderData lagtInnAv;
    boolean avtalt;
    boolean historisk;

    public static KafkaAktivitetMeldingV3 of(AktivitetData aktivitet) {
        AktivitetTypeDTO typeDTO = typeMap.get(aktivitet.getAktivitetType());
        SisteEndringKategori sisteEndringKategori = SisteEndringKategori.getKategori(aktivitet.getStatus(), typeDTO, aktivitet.getTransaksjonsType());

        return KafkaAktivitetMeldingV3.builder()
                .aktivitetId(String.valueOf(aktivitet.getId()))
                .version(aktivitet.getVersjon())
                .aktorId(aktivitet.getAktorId())
                .fraDato(aktivitet.getFraDato())
                .tilDato(aktivitet.getTilDato())
                .endretDato(aktivitet.getEndretDato())
                .aktivitetType(typeDTO)
                .aktivitetStatus(aktivitet.getStatus())
                .sisteEndringKategori(sisteEndringKategori)
                .lagtInnAv(aktivitet.getLagtInnAv())
                .avtalt(aktivitet.isAvtalt())
                .historisk(aktivitet.getHistoriskDato() != null)
                .build();
    }
}
