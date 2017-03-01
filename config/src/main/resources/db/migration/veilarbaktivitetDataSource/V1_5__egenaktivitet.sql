create table EGENAKTIVITET (
    aktivitet_id number(19, 0),
    hensikt varchar(255),
    egen_type varchar(255),
    CONSTRAINT EGENAKTIVITET_FK FOREIGN KEY (aktivitet_id) REFERENCES AKTIVITET (aktivitet_id)
);
CREATE INDEX EGENAKTIVITET_FK ON EGENAKTIVITET(aktivitet_id);

ALTER TABLE STILLINGSSOK ADD arbeidssted varchar(255);