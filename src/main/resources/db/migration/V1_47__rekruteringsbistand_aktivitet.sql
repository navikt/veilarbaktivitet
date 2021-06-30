alter table STILLING_FRA_NAV
    add (
        soknadsfrist varchar,
        svarfrist timestamp(5),
        arbeidsgiver varchar,
        bestillingsId varchar NOT NULL,
        stillingsId varchar NOT NULL,
        arbeidssted varchar,
        varselid varchar
        );

create index STILLING_FRA_NAV_BESTILLINGSID_IDX ON STILLING_FRA_NAV(BESTILLINGSID);
