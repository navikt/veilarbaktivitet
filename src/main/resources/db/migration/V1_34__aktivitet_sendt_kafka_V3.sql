
CREATE TABLE AKTIVITET_SENDT_PAA_KAFKA_V3 (
    AKTIVITET_ID NUMBER(19) NOT NULL,
    AKTIVITET_VERSJON NUMBER(19) NOT NULL,
    SENDT TIMESTAMP not null,
    "OFFSET" NUMBER(19) NOT NULL,

    CONSTRAINT AKTIVITET_KAFKA_V3_PK PRIMARY KEY (AKTIVITET_ID, AKTIVITET_VERSJON),
    CONSTRAINT KAFKA_V3_TIL_AKTIVITET FOREIGN KEY (AKTIVITET_ID, AKTIVITET_VERSJON) REFERENCES AKTIVITET(AKTIVITET_ID, VERSJON)
);

drop table AKTIVITET_SENDT_PAA_KAFKA;
