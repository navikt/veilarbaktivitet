create table ENDRINGSLOGG (
    id number(19, 0) not null,
    aktivitet_id number(19, 0) not null,
    endrings_beskrivelse varchar(255),
    endret_av varchar(255),
    endrings_dato timestamp,
    CONSTRAINT ENDRINGSLOGG_FK FOREIGN KEY (aktivitet_id) REFERENCES AKTIVITET (aktivitet_id)
);
create sequence ENDRINGSLOGG_ID_SEQ start with 1 increment by 1;
CREATE INDEX ENDRINGSLOGG_AKTIVITET_IDX ON ENDRINGSLOGG(aktivitet_id);