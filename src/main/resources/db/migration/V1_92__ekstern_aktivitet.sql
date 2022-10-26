CREATE TABLE EKSTERNAKTIVITET
(
    AKTIVITET_ID NUMBER(19),
    VERSJON      NUMBER(19) DEFAULT 0 NOT NULL,
    SOURCE       NVARCHAR2(255),
    TILTAK_KODE  NVARCHAR2(255),
    AKTIVITETKORT_TYPE         NVARCHAR2(255),
    OPPGAVE      JSON,
    HANDLINGER   JSON,
    DETALJER     JSON,
    ETIKETTER    JSON,
    CONSTRAINT EKSTERNAKTIVITET_FK FOREIGN KEY (AKTIVITET_ID, VERSJON) REFERENCES AKTIVITET,
    CONSTRAINT EKSTERNAKTIVITET_PK PRIMARY KEY (AKTIVITET_ID, VERSJON)
);

DROP TABLE TILTAKSAKTIVITET;