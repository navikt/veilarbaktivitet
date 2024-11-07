package no.nav.veilarbaktivitet.motesms

import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.AktivitetService
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.aktivitet.dto.KanalDTO
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAsserts
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAssertsConfig
import no.nav.veilarbaktivitet.brukernotifikasjon.OpprettVarselDto
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.SendMinsideVarselFraOutboxCron
import no.nav.veilarbaktivitet.db.DbTestUtils
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Date


internal class MoteSmsTest(
    @Autowired
    val moteSMSService: MoteSMSService,
    @Autowired
    val brukernotifikasjonAssertsConfig: BrukernotifikasjonAssertsConfig,
    @Autowired
    val sendMinsideVarselFraOutboxCron: SendMinsideVarselFraOutboxCron,
    @Autowired
    val aktivitetService: AktivitetService
) : SpringBootTestBase() {
    var brukernotifikasjonAsserts: BrukernotifikasjonAsserts? = null
    @BeforeEach
    fun setUp() {
        DbTestUtils.cleanupTestDb(jdbcTemplate)
        brukernotifikasjonAsserts = BrukernotifikasjonAsserts(brukernotifikasjonAssertsConfig)
    }

    @Test
    fun skalSendeServiceMelding() {
        val happyBruker = navMockService.createBruker(BrukerOptions.happyBruker())
        val veileder = navMockService.createVeileder(happyBruker)
        val aktivitetDTO = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE)
        val startTid = ZonedDateTime.now().plusHours(2)
        aktivitetDTO.setFraDato(Date(startTid.toInstant().toEpochMilli()))
        aktivitetDTO.setKanal(KanalDTO.OPPMOTE)
        val mote = aktivitetTestService.opprettAktivitet(happyBruker, veileder, aktivitetDTO)

        moteSmsCronjobber()
        val orginalMelding =
            assertForventetMeldingSendt("Varsel skal ha innhold", happyBruker, KanalDTO.OPPMOTE, startTid, mote)
        brukernotifikasjonAsserts!!.assertSkalIkkeHaProdusertFlereMeldinger()

        moteSmsCronjobber()
        brukernotifikasjonAsserts!!.assertSkalIkkeHaProdusertFlereMeldinger()

        val nyKanal = aktivitetTestService.oppdaterAktivitetOk(happyBruker, veileder, mote.setKanal(KanalDTO.TELEFON))
        moteSmsCronjobber()
        harAvsluttetVarsel(orginalMelding)
        val ny_kanal_varsel =
            assertForventetMeldingSendt("Varsel skal ha nyKanal", happyBruker, KanalDTO.TELEFON, startTid, mote)


        val ny_startTid = startTid.plusHours(2)
        val nyTid = aktivitetTestService.oppdaterAktivitetOk(
            happyBruker,
            veileder,
            nyKanal.setFraDato(Date(ny_startTid.toInstant().toEpochMilli()))
        )

        moteSmsCronjobber()
        harAvsluttetVarsel(ny_kanal_varsel)
        assertForventetMeldingSendt("Varsel skal ha tid", happyBruker, KanalDTO.TELEFON, ny_startTid, mote)

        aktivitetTestService.oppdaterAktivitetOk(
            happyBruker,
            veileder,
            nyTid.setTittel("ny test tittel skal ikke oppdatere varsel")
        )
        moteSmsCronjobber()
        brukernotifikasjonAsserts!!.assertSkalIkkeHaProdusertFlereMeldinger() //"skal ikke sende på nytt for andre oppdateringer"
    }

    @Test
    fun skalSendeForAlleMoteTyper() {
        val happyBruker = navMockService.createBruker(BrukerOptions.happyBruker())
        val veileder = navMockService.createVeileder(happyBruker)
        var aktivitetDTO = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE)
        val fraDato = ZonedDateTime.now().plusHours(4)
        aktivitetDTO.setFraDato(Date(fraDato.toInstant().toEpochMilli()))

        for (kanal in KanalDTO.entries) {
            val aktivitet: AktivitetDTO = aktivitetDTO.setKanal(kanal)
            val response = aktivitetTestService.opprettAktivitet(happyBruker, veileder, aktivitet)

            moteSmsCronjobber()
            assertForventetMeldingSendt(kanal.name + "skal ha riktig melding", happyBruker, kanal, fraDato, response)
            brukernotifikasjonAsserts!!.assertSkalIkkeHaProdusertFlereMeldinger()
        }
    }

    @Test
    fun bareSendeForMote() {
        val happyBruker = navMockService.createBruker(BrukerOptions.happyBruker())
        val veileder = navMockService.createVeileder(happyBruker)
        for (type in AktivitetTypeDTO.entries) {
            if (type == AktivitetTypeDTO.STILLING_FRA_NAV) {
                aktivitetTestService.opprettStillingFraNav(happyBruker)
            } else if (type == AktivitetTypeDTO.EKSTERNAKTIVITET) {
                // TODO aktivitetTestService.opprettEksternAktivitet(happyBruker)
            } else {
                val aktivitet = AktivitetDtoTestBuilder.nyAktivitet(type)
                aktivitet.setFraDato(Date(ZonedDateTime.now().plusHours(4).toInstant().toEpochMilli()))
                aktivitetTestService.opprettAktivitet(happyBruker, veileder, aktivitet)
            }
        }

        moteSmsCronjobber()
        // Blir først sendt en oppgave på stilling-fra-nav
        brukernotifikasjonAsserts!!.assertOppgaveSendt(happyBruker.getFnrAsFnr())
        // Blir sendt en beskjed på mote
        brukernotifikasjonAsserts!!.assertBeskjedSendt(happyBruker.getFnrAsFnr())
        brukernotifikasjonAsserts!!.assertSkalIkkeHaProdusertFlereMeldinger()
        val antall = jdbcTemplate.queryForObject<Int>("Select count(*) from GJELDENDE_MOTE_SMS", Int::class.java)
        Assertions.assertEquals(1, antall)
    }

    @Test
    fun skalFjereneGamleMoter() {
        val happyBruker = navMockService.createBruker(BrukerOptions.happyBruker())
        val veileder = navMockService.createVeileder(happyBruker)
        val aktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE)
        val startTid = ZonedDateTime.now().minusDays(10)
        aktivitet.setFraDato(Date(startTid.toInstant().toEpochMilli()))
        val response = aktivitetTestService.opprettAktivitet(happyBruker, veileder, aktivitet)

        moteSMSService!!.sendServicemeldinger(Duration.ofDays(-15), Duration.ofDays(0))
        sendMinsideVarselFraOutboxCron!!.sendBrukernotifikasjoner()
        val varsel = assertForventetMeldingSendt(
            "skall ha opprettet gamelt varsel",
            happyBruker,
            KanalDTO.OPPMOTE,
            startTid,
            response
        )

        moteSmsCronjobber()

        harAvsluttetVarsel(varsel)
        brukernotifikasjonAsserts!!.assertSkalIkkeHaProdusertFlereMeldinger()
    }

    @Test
    fun skalIkkeOppreteVarsleHistorisk() {
        val happyBruker = navMockService.createBruker(BrukerOptions.happyBruker())
        val veileder = navMockService.createVeileder(happyBruker)
        val aktivitetDTO = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE)
        val startTid = ZonedDateTime.now().plusHours(2)
        aktivitetDTO.setFraDato(Date(startTid.toInstant().toEpochMilli()))
        aktivitetDTO.setKanal(KanalDTO.OPPMOTE)
        aktivitetTestService.opprettAktivitet(happyBruker, veileder, aktivitetDTO)
        aktivitetService!!.settAktiviteterTilHistoriske(happyBruker.getOppfolgingsperiodeId(), ZonedDateTime.now())


        moteSmsCronjobber()
        val antall = jdbcTemplate.queryForObject<Int>("Select count(*) from GJELDENDE_MOTE_SMS", Int::class.java)
        Assertions.assertEquals(0, antall)
        brukernotifikasjonAsserts!!.assertSkalIkkeHaProdusertFlereMeldinger()
    }

    @Test
    fun skalIkkeOppreteVarsleFulfort() {
        val happyBruker = navMockService.createBruker(BrukerOptions.happyBruker())
        val veileder = navMockService.createVeileder(happyBruker)
        val aktivitetDTO = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE)
        val startTid = ZonedDateTime.now().plusHours(2)
        aktivitetDTO.setFraDato(Date(startTid.toInstant().toEpochMilli()))
        val mote = aktivitetTestService.opprettAktivitet(happyBruker, veileder, aktivitetDTO)
        aktivitetTestService.oppdaterAktivitetStatus(happyBruker, veileder, mote, AktivitetStatus.FULLFORT)

        moteSmsCronjobber()
        val antall = jdbcTemplate.queryForObject<Int>("Select count(*) from GJELDENDE_MOTE_SMS", Int::class.java)
        Assertions.assertEquals(0, antall)
        brukernotifikasjonAsserts!!.assertSkalIkkeHaProdusertFlereMeldinger()
    }

    @Test
    fun skalIkkeOppreteVarsleAvbrutt() {
        val happyBruker = navMockService.createBruker(BrukerOptions.happyBruker())
        val veileder = navMockService.createVeileder(happyBruker)
        val aktivitetDTO = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE)
        val startTid = ZonedDateTime.now().plusHours(2)
        aktivitetDTO.setFraDato(Date(startTid.toInstant().toEpochMilli()))
        val mote = aktivitetTestService.opprettAktivitet(happyBruker, veileder, aktivitetDTO)
        aktivitetTestService.oppdaterAktivitetStatus(happyBruker, veileder, mote, AktivitetStatus.AVBRUTT)

        moteSmsCronjobber()
        val antall = jdbcTemplate.queryForObject<Int>("Select count(*) from GJELDENDE_MOTE_SMS", Int::class.java)
        Assertions.assertEquals(0, antall)
        brukernotifikasjonAsserts!!.assertSkalIkkeHaProdusertFlereMeldinger()
    }


    private fun harAvsluttetVarsel(varsel: OpprettVarselDto) {
        brukernotifikasjonAsserts!!.assertInaktivertMeldingErSendt(varsel.varselId)
    }

    private fun moteSmsCronjobber() {
        moteSMSService.stopMoteSms()
        moteSMSService.sendMoteSms()
    }

    private fun assertForventetMeldingSendt(
        melding: String?,
        happyBruker: MockBruker,
        oppmote: KanalDTO?,
        startTid: ZonedDateTime?,
        mote: AktivitetDTO
    ): OpprettVarselDto {
        val oppgave = brukernotifikasjonAsserts!!.assertBeskjedSendt(happyBruker.getFnrAsFnr())

        val expected = MoteNotifikasjon(0L, 0L, happyBruker.getAktorIdAsAktorId(), oppmote, startTid)
        Assertions.assertEquals(happyBruker.getFnr(), oppgave.ident, melding + " fnr")
        Assertions.assertNotNull(oppgave.eksternVarsling, melding + " eksternvarsling")
        Assertions.assertEquals(
            expected.getSmsTekst(),
            oppgave.eksternVarsling.smsVarslingstekst,
            melding + " sms tekst"
        )
        Assertions.assertEquals(expected.getDitNavTekst(), oppgave.tekster[0].tekst, melding + " ditnav tekst")
        Assertions.assertEquals(
            expected.getEpostTitel(),
            oppgave.eksternVarsling.epostVarslingstittel,
            melding + " epost tittel tekst"
        )
        Assertions.assertEquals(
            expected.getEpostBody(),
            oppgave.eksternVarsling.epostVarslingstekst,
            melding + " epost body tekst"
        )
        Assertions.assertTrue(
            oppgave.link.contains(mote.getId()),
            melding + " mote link tekst"
        ) //TODO burde lage en test metode for aktivitets linker
        return oppgave
    }
}
