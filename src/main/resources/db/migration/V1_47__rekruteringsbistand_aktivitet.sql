alter table STILLING_FRA_NAV
    add (
        soknadsfrist varchar(255),
        svarfrist timestamp(5),
        arbeidsgiver varchar(255),
        bestillingsId varchar(255) NOT NULL,
        stillingsId varchar(255) NOT NULL,
        arbeidssted varchar(255),
        varselid varchar(255)
        );

create index STILLING_FRA_NAV_BESTILLINGSID_IDX ON STILLING_FRA_NAV(BESTILLINGSID);
