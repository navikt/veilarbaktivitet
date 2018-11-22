INSERT INTO AKTIVITET_TYPE (AKTIVITET_TYPE_KODE, OPPRETTET_DATO, OPPRETTET_AV)
VALUES ('MOTE', CURRENT_TIMESTAMP, 'KASSERT');

INSERT INTO AKTIVITET_TYPE (AKTIVITET_TYPE_KODE, OPPRETTET_DATO, OPPRETTET_AV)
VALUES ('SAMTALEREFERAT', CURRENT_TIMESTAMP, 'KASSERT');

INSERT INTO TRANSAKSJONS_TYPE (TRANSAKSJONS_TYPE_KODE, OPPRETTET_DATO, OPPRETTET_AV)
VALUES ('REFERAT_ENDRET', CURRENT_TIMESTAMP, 'KASSERT');

INSERT INTO TRANSAKSJONS_TYPE (TRANSAKSJONS_TYPE_KODE, OPPRETTET_DATO, OPPRETTET_AV)
VALUES ('REFERAT_PUBLISERT', CURRENT_TIMESTAMP, 'KASSERT');

CREATE TABLE KANAL_TYPE
(
  KANAL_TYPE_KODE        NVARCHAR2(255) NOT NULL
    CONSTRAINT KANAL_TYPE_PK
    PRIMARY KEY,
  OPPRETTET_DATO      TIMESTAMP(6)   NOT NULL,
  OPPRETTET_AV        NVARCHAR2(255) NOT NULL,
  ENDRET_DATO         TIMESTAMP(6),
  ENDRET_AV           NVARCHAR2(255)
);

INSERT INTO KANAL_TYPE (KANAL_TYPE_KODE, OPPRETTET_DATO, OPPRETTET_AV)
VALUES ('TELEFON', CURRENT_TIMESTAMP, 'KASSERT');

INSERT INTO KANAL_TYPE (KANAL_TYPE_KODE, OPPRETTET_DATO, OPPRETTET_AV)
VALUES ('INTERNETT', CURRENT_TIMESTAMP, 'KASSERT');

INSERT INTO KANAL_TYPE (KANAL_TYPE_KODE, OPPRETTET_DATO, OPPRETTET_AV)
VALUES ('OPPMOTE', CURRENT_TIMESTAMP, 'KASSERT');


CREATE TABLE MOTE (
  AKTIVITET_ID      NUMBER(19),
  VERSJON           NUMBER(19) DEFAULT 0 NOT NULL,
  ADRESSE           NVARCHAR2(255),
  FORBEREDELSER     CLOB,
  KANAL             NVARCHAR2(255)
    CONSTRAINT KANAL_TYPE_FK
    REFERENCES KANAL_TYPE,
  REFERAT           CLOB,
  REFERAT_PUBLISERT NUMBER(1) DEFAULT 0  NOT NULL,
  CONSTRAINT MOTE_FK FOREIGN KEY (AKTIVITET_ID, VERSJON) REFERENCES AKTIVITET,
  CONSTRAINT MOTE_PK PRIMARY KEY (AKTIVITET_ID, VERSJON)
);
