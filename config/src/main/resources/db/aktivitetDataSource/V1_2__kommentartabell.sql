create table KOMMENTAR (
    aktivitet_id number(19, 0),
    kommentar varchar(255),
    opprettet_av varchar(255),
    opprettet_dato varchar(255),
    CONSTRAINT KOMMENTAR_AKTIVITET_FK FOREIGN KEY (aktivitet_id) REFERENCES AKTIVITET (aktivitet_id)
);
CREATE INDEX KOMMENTAR_AKTIVITET_IDX ON KOMMENTAR(aktivitet_id);