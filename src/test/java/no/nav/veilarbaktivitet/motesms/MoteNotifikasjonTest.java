package no.nav.veilarbaktivitet.motesms;

import no.nav.veilarbaktivitet.aktivitet.dto.KanalDTO;
import no.nav.veilarbaktivitet.person.Person;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoteNotifikasjonTest {
    private final ZonedDateTime startTid = ZonedDateTime.of(LocalDate.of(2022, 2, 14), LocalTime.of(14, 42), TimeZone.getTimeZone("CET").toZoneId());


    private MoteNotifikasjon create(KanalDTO kanalDTO, ZonedDateTime moteTid) {
        return new MoteNotifikasjon(1L, 2L, Person.aktorId("12345678"), kanalDTO, moteTid);
    }

    @Test
    void skalHa24TimerKlokke() {
        ZonedDateTime startTidMed = ZonedDateTime.of(LocalDate.of(2022, 2, 14), LocalTime.of(14, 42), TimeZone.getTimeZone("CET").toZoneId());
        MoteNotifikasjon moteNotifikasjon = create(null, startTidMed);
        assertEquals("mandag 14. februar kl. 14:42", moteNotifikasjon.getMoteTid());
    }

    @Test
    void skalPaddeTidspunktMedNull() {
        ZonedDateTime startTidMed = ZonedDateTime.of(LocalDate.of(2022, 1, 10), LocalTime.of(1, 1), TimeZone.getTimeZone("CET").toZoneId());
        MoteNotifikasjon moteNotifikasjon = create(null, startTidMed);
        assertEquals("mandag 10. januar kl. 01:01", moteNotifikasjon.getMoteTid());
    }

    @Test
    void oppmoteTekst() {
        MoteNotifikasjon oppmote = create(KanalDTO.OPPMOTE, startTid);
        assertEquals("Vi minner om at du har et møte mandag 14. februar kl. 14:42", oppmote.getSmsTekst());
        assertEquals("Vi minner om at du har et møte mandag 14. februar kl. 14:42", oppmote.getDitNavTekst());
        assertEquals("Påminnelse om møte", oppmote.getEpostTitel());
        assertEquals("Vi minner om at du har et møte mandag 14. februar kl. 14:42 \nVennlig hilsen Nav", oppmote.getEpostBody());
    }

    @Test
    void telefonoTekst() {
        MoteNotifikasjon oppmote = create(KanalDTO.TELEFON, startTid);
        assertEquals("Vi minner om at du har et telefonmøte mandag 14. februar kl. 14:42", oppmote.getSmsTekst());
        assertEquals("Vi minner om at du har et telefonmøte mandag 14. februar kl. 14:42", oppmote.getDitNavTekst());
        assertEquals("Påminnelse om møte", oppmote.getEpostTitel());
        assertEquals("Vi minner om at du har et telefonmøte mandag 14. februar kl. 14:42 \nVennlig hilsen Nav", oppmote.getEpostBody());
    }

    @Test
    void nettTekst() {
        MoteNotifikasjon oppmote = create(KanalDTO.INTERNETT, startTid);
        assertEquals("Vi minner om at du har et videomøte mandag 14. februar kl. 14:42", oppmote.getSmsTekst());
        assertEquals("Vi minner om at du har et videomøte mandag 14. februar kl. 14:42", oppmote.getDitNavTekst());
        assertEquals("Påminnelse om møte", oppmote.getEpostTitel());
        assertEquals("Vi minner om at du har et videomøte mandag 14. februar kl. 14:42 \nVennlig hilsen Nav", oppmote.getEpostBody());
    }
}
