    create table GJELDENDE_MOTE_SMS
(
    AKTIVITET_ID Number(19)   not null,
    MOTETID      timestamp(6) not null
);


create table MOTE_SMS_HISTORIKK
(
    AKTIVITET_ID      Number(19)     not null,
    AKTIVITET_VERSJON Number(19)     not null,
    MOTETID           timestamp(6)   not null,
    VARSEL_ID         nvarchar2(255) not null,
    SENDT             timestamp(6)   not null
);

create index GJELDENDE_TYPE_TID_AKTIVITET_idx on AKTIVITET
(
    GJELDENDE,
    FRA_DATO,
    AKTIVITET_TYPE_KODE
);

create index GJELDENDE_MOTE_SMS_idx on GJELDENDE_MOTE_SMS (AKTIVITET_ID);
