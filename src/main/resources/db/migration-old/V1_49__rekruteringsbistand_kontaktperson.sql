alter table STILLING_FRA_NAV
    add (
        kontaktperson_navn varchar(255) not null,
        kontaktperson_tittel varchar(255) not null,
        kontaktperson_mobil varchar(255),
        kontaktperson_epost varchar(255)
        );
