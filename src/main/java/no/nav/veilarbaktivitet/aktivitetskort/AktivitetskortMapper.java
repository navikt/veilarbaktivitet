package no.nav.veilarbaktivitet.aktivitetskort;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.aktivitet.domain.TiltaksaktivitetData;
import org.apache.commons.lang3.NotImplementedException;

import static no.nav.veilarbaktivitet.util.DateUtils.toDate;

public class AktivitetskortMapper {

    private AktivitetskortMapper() {
    }

    public static AktivitetData map(ActionType actionType, JsonNode payload) {


        AktivitetData aktivitetData = switch (actionType) {
            case UPSERT_TILTAK_AKTIVITET_V1 -> {
                TiltaksaktivitetDTO tiltaksaktivitetDTO = JsonUtils.fromJson(payload.toString(), TiltaksaktivitetDTO.class);
                yield mapTilAktivitetData(tiltaksaktivitetDTO);
            }
            case UPSERT_GRUPPE_AKTIVITET_V1, UPSERT_UTDANNING_AKTIVITET_V1 -> throw new NotImplementedException("todo");
        };

        return aktivitetData;
    }

    private static AktivitetData mapTilAktivitetData(TiltaksaktivitetDTO tiltaksaktivitetDTO) {
        var build = AktivitetData.builder()
                .funksjonellId(tiltaksaktivitetDTO.id)
                .aktorId(tiltaksaktivitetDTO.personIdent)
                .tittel(tiltaksaktivitetDTO.tittel)
                .fraDato(toDate(tiltaksaktivitetDTO.startDato))
                .tilDato(toDate(tiltaksaktivitetDTO.sluttDato))
                .beskrivelse(tiltaksaktivitetDTO.beskrivelse)
                .status(tiltaksaktivitetDTO.aktivitetStatus)
                .tiltaksaktivitetData(mapTiltaksaktivitet(tiltaksaktivitetDTO))
                .aktivitetType(AktivitetTypeData.TILTAKSAKTIVITET)
                .lagtInnAv(tiltaksaktivitetDTO.endretAv.identType().mapToInnsenderType())
                .build();
        return build;
    }

    private static TiltaksaktivitetData mapTiltaksaktivitet(TiltaksaktivitetDTO tiltaksaktivitetDTO) {
        var tiltaksaktivitetData = TiltaksaktivitetData.builder()
                .tiltakskode(tiltaksaktivitetDTO.tiltaksKode)
                .tiltaksnavn(tiltaksaktivitetDTO.tiltaksNavn)
                .arrangornavn(tiltaksaktivitetDTO.arrangoernavn)
                .deltakelseStatus(tiltaksaktivitetDTO.deltakelseStatus)
                .dagerPerUke(Integer.parseInt(tiltaksaktivitetDTO.getDetaljer().get("dagerPerUke")))
                .deltakelsesprosent(Integer.parseInt(tiltaksaktivitetDTO.getDetaljer().get("deltakelsesprosent")))
                .build();

        return tiltaksaktivitetData;
    }
}
