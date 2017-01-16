create table AKTIVITET (
    aktivitet_id number(19, 0) not null,
    aktor_id varchar(255),
    tittel varchar(255),
    type varchar(255),
    beskrivelse varchar(255),
    status varchar(255),
    avsluttet_dato timestamp,
    avsluttet_kommentar varchar(255),
    lagt_inn_av varchar(255),
    fra_dato timestamp,
    til_dato timestamp,
    lenke varchar(255),
    dele_med_nav number(1,0),
    opprettet_dato timestamp,
    constraint AKTIVITET_PK primary key (aktivitet_id)
);
create sequence AKTIVITET_ID_SEQ start with 1 increment by 1;
CREATE INDEX AKTIVITET_AKTOR_IDX ON AKTIVITET(aktor_id);