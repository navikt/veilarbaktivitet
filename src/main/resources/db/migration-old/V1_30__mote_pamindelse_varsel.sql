    create table GJELDENDE_MOTE_SMS
(
    AKTIVITET_ID Number(19)   not null,
    MOTETID      timestamp(6) not null,

    CONSTRAINT GJELDENDE_MOTE_SMS_PK  PRIMARY KEY (AKTIVITET_ID)
);


    create table MOTE_SMS_HISTORIKK
    (
        AKTIVITET_ID      Number(19)     not null,
        VERSJON Number(19)     not null,
        MOTETID           timestamp(6)   not null,
        VARSEL_ID         nvarchar2(255) not null unique,
        SENDT             timestamp(6)   not null,

        CONSTRAINT MOTE_SMS_HISTORIKK_PK  PRIMARY KEY (AKTIVITET_ID, VERSJON),
        CONSTRAINT MSH_AKTIVITET_FK  FOREIGN KEY (AKTIVITET_ID, VERSJON) REFERENCES AKTIVITET
    );



create index AKTIVTET_SMSQUERY_idx on AKTIVITET
(
    GJELDENDE,
    FRA_DATO,
    LIVSLOPSTATUS_KODE,
    AKTIVITET_TYPE_KODE
);

