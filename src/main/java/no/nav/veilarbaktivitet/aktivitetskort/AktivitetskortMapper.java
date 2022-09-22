package no.nav.veilarbaktivitet.aktivitetskort;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData.AktivitetDataBuilder;
import no.nav.veilarbaktivitet.aktivitet.domain.TiltaksaktivitetData;
import no.nav.veilarbaktivitet.util.DateUtils;
import org.apache.commons.lang3.NotImplementedException;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

import static no.nav.veilarbaktivitet.util.DateUtils.toDate;

public class AktivitetskortMapper {

    private AktivitetskortMapper () {}

    public static AktivitetData map(ActionType actionType, JsonNode payload) {
        AktivitetDataBuilder aktivitetDataBuilder = AktivitetData.builder();

        switch (actionType) {
            case UPSERT_TILTAK_AKTIVITET_V1 -> {
                TiltaksaktivitetDTO tiltaksaktivitetDTO = JsonUtils.fromJson(payload.toString(), TiltaksaktivitetDTO.class);
                mapTiltaksaktivitet(aktivitetDataBuilder, tiltaksaktivitetDTO);
            }
            case UPSERT_GRUPPE_AKTIVITET_V1, UPSERT_UTDANNING_AKTIVITET_V1 -> {
                throw new NotImplementedException("todo");
            }
        }

        return aktivitetDataBuilder.build();
    }

    private static AktivitetDataBuilder mapTiltaksaktivitet(AktivitetDataBuilder builder, TiltaksaktivitetDTO tiltaksaktivitetDTO) {
        TiltaksaktivitetData tiltaksaktivitetData = TiltaksaktivitetData.builder()
                .tiltakskode(tiltaksaktivitetDTO.tiltakDTO.kode)
                .tiltaksnavn(tiltaksaktivitetDTO.tiltakDTO.navn)
                .arrangornavn(tiltaksaktivitetDTO.arrangornavn)
                .aarsak(tiltaksaktivitetDTO.statusDTO.aarsak)
                .deltakelsesprosent(tiltaksaktivitetDTO.deltakelsesprosent)
                .dagerPerUke(tiltaksaktivitetDTO.dagerPerUke)
                .registrertDato(tiltaksaktivitetDTO.registrertDato)
                .statusEndretDato(tiltaksaktivitetDTO.statusEndretDato)
                .build();

        return builder
                .funksjonellId(tiltaksaktivitetDTO.funksjonellId)
                .aktorId(tiltaksaktivitetDTO.personIdent)
                .tittel(tiltaksaktivitetDTO.tittel)
                .fraDato(toDate(tiltaksaktivitetDTO.startDato))
                .tilDato(toDate(tiltaksaktivitetDTO.sluttDato))
                .beskrivelse(tiltaksaktivitetDTO.beskrivelse)
                .status(tiltaksaktivitetDTO.statusDTO.status)
                .tiltaksaktivitetData(tiltaksaktivitetData);
    }
}
