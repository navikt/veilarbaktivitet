alter table BRUKERNOTIFIKASJON
    add varsel_kvittering_status varchar(255) default ('IKKE_SATT') not null;