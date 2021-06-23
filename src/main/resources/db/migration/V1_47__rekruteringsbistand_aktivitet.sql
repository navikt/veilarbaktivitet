alter table STILLING_FRA_NAV
    add (
        soknadsfrist varchar,
        svarfrist timestamp(5),
        arbeidsgiver varchar,
        bestillingsId varchar,
        stillingsId varchar,
        arbeidssted varchar,
        varselid varchar
        );
