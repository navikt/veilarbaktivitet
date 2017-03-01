create table STILLINGSSOK (
    aktivitet_id number(19, 0),
    arbeidsgiver varchar(255),
    stillingstittel varchar(255),
    kontaktperson varchar(255),
    etikett varchar(255),
    CONSTRAINT STILLINGSSOK_AKTIVITET_FK FOREIGN KEY (aktivitet_id) REFERENCES AKTIVITET (aktivitet_id)
);
CREATE INDEX STILLINGSSOK_AKTIVITET_IDX ON STILLINGSSOK(aktivitet_id);