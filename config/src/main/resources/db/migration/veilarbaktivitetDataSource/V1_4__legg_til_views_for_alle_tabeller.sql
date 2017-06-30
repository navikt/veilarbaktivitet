CREATE VIEW DVH_TRANSAKSJON_TYPE AS (
  SELECT
    TRANSAKSJONS_TYPE_KODE,
    OPPRETTET_DATO,
    OPPRETTET_AV,
    ENDRET_DATO,
    ENDRET_AV
  FROM TRANSAKSJONS_TYPE
);

CREATE VIEW DVH_AKT_LIVSLOPSTATUS_TYPE AS (
  SELECT
    AKTIVITET_LIVSLOP_KODE,
    OPPRETTET_DATO,
    OPPRETTET_AV,
    ENDRET_DATO,
    ENDRET_AV
  FROM AKTIVITET_LIVSLOPSTATUS_TYPE
);

CREATE VIEW DVH_AKTIVITET_TYPE AS (
  SELECT
    AKTIVITET_TYPE_KODE,
    OPPRETTET_DATO,
    OPPRETTET_AV,
    ENDRET_DATO,
    ENDRET_AV
  FROM AKTIVITET_TYPE
);

CREATE VIEW DVH_STILLINGSOK_ETIKETT_TYPE AS (
  SELECT
    ETIKETT_KODE,
    OPPRETTET_DATO,
    OPPRETTET_AV,
    ENDRET_DATO,
    ENDRET_AV
  FROM STILLINGSSOK_ETIKETT_TYPE
);

CREATE VIEW DVH_AKTIVITET AS (
  SELECT
    AKTIVITET_ID,
    VERSJON,
    TRANSAKSJONS_TYPE,
    AKTOR_ID,
    TITTEL,
    TYPE,
    AVSLUTTET_DATO,
    AVSLUTTET_KOMMENTAR,
    LAGT_INN_AV,
    FRA_DATO,
    TIL_DATO,
    LENKE,
    OPPRETTET_DATO,
    ENDRET_DATO,
    ENDRET_AV,
    STATUS,
    BESKRIVELSE,
    AVTALT,
    HISTORISK_DATO,
    GJELDENDE
  FROM AKTIVITET
);

CREATE VIEW DVH_STILLINGSOK_AKTIVITET AS (
  SELECT
    STILLINGSSOK.AKTIVITET_ID,
    STILLINGSSOK.VERSJON,
    STILLINGSSOK.ARBEIDSGIVER,
    STILLINGSSOK.STILLINGSTITTEL,
    STILLINGSSOK.KONTAKTPERSON,
    STILLINGSSOK.ETIKETT,
    STILLINGSSOK.ARBEIDSSTED,
    AKTIVITET.ENDRET_DATO
  FROM STILLINGSSOK
  LEFT JOIN AKTIVITET ON STILLINGSSOK.AKTIVITET_ID = AKTIVITET.AKTIVITET_ID AND STILLINGSSOK.VERSJON = AKTIVITET.VERSJON
);

CREATE VIEW DVH_SOKEAVTALE_AKTIVITET AS (
  SELECT
    AKTIVITET_ID,
    VERSJON,
    ANTALL,
    AVTALE_OPPFOLGING
  FROM SOKEAVTALE
);

CREATE VIEW DVH_EGENAKTIVITET AS (
  SELECT
    EGENAKTIVITET.AKTIVITET_ID,
    EGENAKTIVITET.VERSJON,
    EGENAKTIVITET.HENSIKT,
    EGENAKTIVITET.OPPFOLGING,
    AKTIVITET.ENDRET_DATO
  FROM EGENAKTIVITET
  LEFT JOIN AKTIVITET ON EGENAKTIVITET.AKTIVITET_ID = AKTIVITET.AKTIVITET_ID AND EGENAKTIVITET.VERSJON = AKTIVITET.VERSJON
);