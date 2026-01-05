package no.nav.veilarbaktivitet.aktivitetskort.dto

import org.slf4j.LoggerFactory


/*
      MIDLERTIDIG_LONNSTILSKUDD - Midlertidig lønnstilskudd
      VARIG_LONNSTILSKUDD - Varig lønnstilskudd
      ARBEIDSTRENING - Arbeidstrening
      Mentor - Mentor
      VARIG_TILRETTELAGT_ARBEID_I_ORDINAER_VIRKSOMHET - Varig tilrettelagt arbeid i ordinær virksomhet
      INDOPPFAG - Oppfølging
      ARBFORB - Arbeidsforberedende trening (AFT)
      AVKLARAG  - Avklaring
      VASV  - Varig tilrettelagt arbeid i skjermet virksomhet
      ARBRRHDAG” - Arbeidsrettet rehabilitering (dag)
      DIGIOPPARB - Digitalt jobbsøkerkurs for arbeidsledige (jobbklubb)
      JOBBK - Jobbklubb
      GRUPPEAMO - Gruppe AMO (arbeidsmarkedsopplæring)
      GRUFAGYRKE  - Gruppe Fag- og yrkesopplæring VGS og høyere yrkesfaglig utdanning
      ARENA_TILTAK - Resten av arbeidsmarkedstiltakene i arena (>100)
      ENKELAMO -> "Arbeidsmarkedsopplæring (enkeltplass)" (fases sakte ut)
      ENKFAGYRKE -> "Fag- og yrkesopplæring" (skal fases sakte ut)
      HOYEREUTD -> "Høyere utdanning"
      ARBEIDSMARKEDSOPPLAERING - GRUPPEAMO og ENKELAMO etter ny forskrift (bokstav a)
      NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV - GRUPPEAMO og ENKELAMO etter ny forskrift (bokstav b)
      STUDIESPESIALISERING - Ny tiltakskode for GRUPPEAMO og ENKELAMO etter ny forskrift (bokstav c)
      FAG_OG_YRKESOPPLAERING - GRUFAGYRK og ENKFAGYRK etter ny forskrift(bokstav d)
      HOYERE_YRKESFAGLIG_UTDANNING - GRUFAGYRK og ENKFAGYRK etter ny forskrift(bokstav d)

*/
enum class AktivitetskortType {
    MIDLERTIDIG_LONNSTILSKUDD,
    VARIG_LONNSTILSKUDD,
    ARBEIDSTRENING,
    MENTOR,
    VARIG_TILRETTELAGT_ARBEID_I_ORDINAER_VIRKSOMHET,
    ARENA_TILTAK,
    INDOPPFAG,
    ARBFORB,
    AVKLARAG,
    VASV,
    ARBRRHDAG,
    DIGIOPPARB,
    JOBBK,
    GRUPPEAMO,
    GRUFAGYRKE,
    REKRUTTERINGSTREFF,
    ENKELAMO,
    ENKFAGYRKE,
    HOYEREUTD,
    ARBEIDSMARKEDSOPPLAERING,
    NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
    STUDIESPESIALISERING,
    FAG_OG_YRKESOPPLAERING,
    HOYERE_YRKESFAGLIG_UTDANNING;

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)

        fun tilAktivitetskortType(string: String): AktivitetskortType {
            try {
                return AktivitetskortType.valueOf(string.uppercase())
            } catch (exception: IllegalArgumentException) {
                logger.error("AktivitetskortType er ukjent verdi: $string", exception)
                throw exception
            }
        }
    }
}
