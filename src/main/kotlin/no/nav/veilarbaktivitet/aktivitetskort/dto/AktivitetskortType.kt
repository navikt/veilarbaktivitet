package no.nav.veilarbaktivitet.aktivitetskort.dto
/*
      MIDLERTIDIG_LONNSTILSKUDD - Midlertidig lønnstilskudd
      VARIG_LONNSTILSKUDD - Varig lønnstilskudd
      INDOPPFAG - Oppfølging
      ARBFORB - Arbeidsforberedende trening (AFT)
      AVKLARAG  - Avklaring
      VASV  - Varig tilrettelagt arbeid i skjermet virksomhet
      ARBRRHDAG” - Arbeidsrettet rehabilitering (dag)
      DIGIOPPARB - Digitalt jobbsøkerkurs for arbeidsledige (jobbklubb)
      JOBBK - Jobbklubb
      GRUPPEAMO Gruppe AMO (arbeidsmarkedsopplæring)
      GRUFAGYRKE  - Gruppe Fag- og yrkesopplæring VGS og høyere yrkesfaglig utdanning
      ARENA_TILTAK - Resten av arbeidsmarkedstiltakene i arena (>100)
 */
enum class AktivitetskortType {
    MIDLERTIDIG_LONNSTILSKUDD,
    VARIG_LONNSTILSKUDD,
    ARENA_TILTAK,
    INDOPPFAG,
    ARBFORB,
    AVKLARAG,
    VASV,
    ARBRRHDAG,
    DIGIOPPARB,
    JOBBK,
    GRUPPEAMO,
    GRUFAGYRKE
}
