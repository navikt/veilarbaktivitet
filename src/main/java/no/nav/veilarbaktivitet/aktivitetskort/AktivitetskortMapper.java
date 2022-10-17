package no.nav.veilarbaktivitet.aktivitetskort;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.aktivitet.domain.TiltaksaktivitetData;
import org.apache.commons.lang3.NotImplementedException;

import java.time.LocalDateTime;

import static no.nav.veilarbaktivitet.util.DateUtils.localDateTimeToDate;
import static no.nav.veilarbaktivitet.util.DateUtils.toDate;

public class AktivitetskortMapper {

    private AktivitetskortMapper() {
    }

    static AktivitetData mapTilAktivitetData(TiltaksaktivitetDTO tiltaksaktivitetDTO, LocalDateTime opprettetDato, LocalDateTime endretDato, String aktorId) {
        return AktivitetData.builder()
                .funksjonellId(tiltaksaktivitetDTO.id)
                .aktorId(aktorId)
                .tittel(tiltaksaktivitetDTO.tittel)
                .fraDato(toDate(tiltaksaktivitetDTO.startDato))
                .tilDato(toDate(tiltaksaktivitetDTO.sluttDato))
                .beskrivelse(tiltaksaktivitetDTO.beskrivelse)
                .status(tiltaksaktivitetDTO.aktivitetStatus)
                .tiltaksaktivitetData(mapTiltaksaktivitet(tiltaksaktivitetDTO))
                .aktivitetType(AktivitetTypeData.TILTAKSAKTIVITET)
                .lagtInnAv(tiltaksaktivitetDTO.endretAv.identType().mapToInnsenderType())
                .opprettetDato(localDateTimeToDate(opprettetDato))
                .endretDato(localDateTimeToDate(endretDato))
                .endretAv(tiltaksaktivitetDTO.endretAv.ident())
                .build();
    }


    private static TiltaksaktivitetData mapTiltaksaktivitet(TiltaksaktivitetDTO tiltaksaktivitetDTO) {
        return TiltaksaktivitetData.builder()
                .tiltakskode(tiltaksaktivitetDTO.tiltaksKode)
                .tiltaksnavn(tiltaksaktivitetDTO.tiltaksNavn)
                .arrangornavn(tiltaksaktivitetDTO.arrangoernavn)
                .deltakelseStatus(tiltaksaktivitetDTO.deltakelseStatus)
                .dagerPerUke(Integer.parseInt(tiltaksaktivitetDTO.getDetaljer().get("dagerPerUke")))
                .deltakelsesprosent(Integer.parseInt(tiltaksaktivitetDTO.getDetaljer().get("deltakelsesprosent")))
                .build();
    }
}
