CREATE TABLE AKTIVITET_TYPE
(
  AKTIVITET_TYPE_KODE        NVARCHAR2(255) NOT NULL
    CONSTRAINT AKTIVITET_TYPE_PK
    PRIMARY KEY,
  OPPRETTET_DATO             TIMESTAMP(6)   NOT NULL,
  OPPRETTET_AV               NVARCHAR2(255) NOT NULL,
  ENDRET_DATO                TIMESTAMP(6),
  ENDRET_AV                  NVARCHAR2(255)
);

CREATE TABLE AKTIVITET_LIVSLOPSTATUS_TYPE
(
  AKTIVITET_LIVSLOP_KODE        NVARCHAR2(255)
    CONSTRAINT AKTIVITET_LIVSLOPSTATUS_TYPE_PK
    PRIMARY KEY,
  OPPRETTET_DATO                TIMESTAMP(6)   NOT NULL,
  OPPRETTET_AV                  NVARCHAR2(255) NOT NULL,
  ENDRET_DATO                   TIMESTAMP(6),
  ENDRET_AV                     NVARCHAR2(255)
);

CREATE TABLE STILLINGSSOK_ETIKETT_TYPE
(
  ETIKETT_KODE        NVARCHAR2(255) NOT NULL
    CONSTRAINT STILLINGSSOK_ETIKETT_TYPE_PK
    PRIMARY KEY,
  OPPRETTET_DATO      TIMESTAMP(6)   NOT NULL,
  OPPRETTET_AV        NVARCHAR2(255) NOT NULL,
  ENDRET_DATO         TIMESTAMP(6),
  ENDRET_AV           NVARCHAR2(255)
);


CREATE TABLE TRANSAKSJONS_TYPE
(
  TRANSAKSJONS_TYPE_KODE        NVARCHAR2(255) NOT NULL
    CONSTRAINT TRANSAKSJONS_TYPE_PK
    PRIMARY KEY,
  OPPRETTET_DATO                TIMESTAMP(6)   NOT NULL,
  OPPRETTET_AV                  NVARCHAR2(255) NOT NULL,
  ENDRET_DATO                   TIMESTAMP(6),
  ENDRET_AV                     NVARCHAR2(255)
);

CREATE TABLE AKTIVITET
(
  AKTIVITET_ID        NUMBER(19)           NOT NULL,
  VERSJON             NUMBER(19) DEFAULT 0 NOT NULL,
  TRANSAKSJONS_TYPE   NVARCHAR2(255)       NOT NULL
    CONSTRAINT TRANSAKSJON_TYPE_FK
    REFERENCES TRANSAKSJONS_TYPE,
  AKTOR_ID            NVARCHAR2(255),
  TITTEL              NVARCHAR2(255),
  TYPE                NVARCHAR2(255)       NOT NULL
    CONSTRAINT AKTIVITET_TYPE_FK
    REFERENCES AKTIVITET_TYPE,
  AVSLUTTET_DATO      TIMESTAMP(6),
  AVSLUTTET_KOMMENTAR NVARCHAR2(255),
  LAGT_INN_AV         NVARCHAR2(255),
  FRA_DATO            TIMESTAMP(6),
  TIL_DATO            TIMESTAMP(6),
  LENKE               CLOB,
  OPPRETTET_DATO      TIMESTAMP(6),
  ENDRET_DATO         TIMESTAMP(6),
  ENDRET_AV           NVARCHAR2(255),
  STATUS              NVARCHAR2(255)       NOT NULL
    CONSTRAINT AKTIVITET_LIVSLOPSTATUS_TYPE_FK
    REFERENCES AKTIVITET_LIVSLOPSTATUS_TYPE,
  BESKRIVELSE         CLOB,
  AVTALT              NUMBER(1) DEFAULT 0  NOT NULL,
  HISTORISK           NUMBER(1) DEFAULT 0  NOT NULL,
  GJELDENDE           NUMBER(1) DEFAULT 0  NOT NULL,
  CONSTRAINT AKTIVITET_PK PRIMARY KEY (AKTIVITET_ID, VERSJON)
);

ALTER TABLE AKTIVITET
  MODIFY LOB (BESKRIVELSE) ( DEDUPLICATE );
ALTER TABLE AKTIVITET
  MODIFY LOB (LENKE) ( DEDUPLICATE );
ALTER TABLE SOKEAVTALE
  MODIFY LOB (AVTALE_OPPFOLGING) ( DEDUPLICATE );

CREATE INDEX AKTIVITET_AKTOR_IDX
  ON AKTIVITET (AKTOR_ID, GJELDENDE);
CREATE INDEX AKTIVITET_ID_IDX
  ON AKTIVITET (AKTIVITET_ID, GJELDENDE);
CREATE SEQUENCE AKTIVITET_ID_SEQ START WITH 1 INCREMENT BY 1;

CREATE TABLE EGENAKTIVITET
(
  AKTIVITET_ID NUMBER(19),
  VERSJON      NUMBER(19) DEFAULT 0 NOT NULL,
  HENSIKT      NVARCHAR2(255),
  OPPFOLGING   NVARCHAR2(255),
  CONSTRAINT EGENAKTIVITET_FK FOREIGN KEY (AKTIVITET_ID, VERSJON) REFERENCES AKTIVITET
);
CREATE INDEX EGENAKTIVITET_FK
  ON EGENAKTIVITET (AKTIVITET_ID, VERSJON);

CREATE TABLE SOKEAVTALE
(
  AKTIVITET_ID      NUMBER(19),
  VERSJON           NUMBER(19) DEFAULT 0 NOT NULL,
  ANTALL            NUMBER(19),
  AVTALE_OPPFOLGING CLOB,
  CONSTRAINT SOKEAVTALE_FK FOREIGN KEY (AKTIVITET_ID, VERSJON) REFERENCES AKTIVITET
);
CREATE INDEX SOKEAVTALE_AKTIVITET_IDX
  ON SOKEAVTALE (AKTIVITET_ID, VERSJON);

CREATE TABLE STILLINGSSOK
(
  AKTIVITET_ID    NUMBER(19),
  VERSJON         NUMBER(19) DEFAULT 0 NOT NULL,
  ARBEIDSGIVER    NVARCHAR2(255),
  STILLINGSTITTEL NVARCHAR2(255),
  KONTAKTPERSON   NVARCHAR2(255),
  ETIKETT         NVARCHAR2(255)
    CONSTRAINT STILLINGSSOK_ETIKETT_TYPE_FK
    REFERENCES STILLINGSSOK_ETIKETT_TYPE,
  ARBEIDSSTED     NVARCHAR2(255),
  CONSTRAINT STILLINGSSOK_AKTIVITET_FK FOREIGN KEY (AKTIVITET_ID, VERSJON) REFERENCES AKTIVITET
);
CREATE INDEX STILLINGSSOK_AKTIVITET_IDX
  ON STILLINGSSOK (AKTIVITET_ID, VERSJON);
