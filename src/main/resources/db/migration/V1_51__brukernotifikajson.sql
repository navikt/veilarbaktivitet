alter table BRUKERNOTIFIKASJON
    rename column SENDT to OPPRETTET;
alter table BRUKERNOTIFIKASJON
    add BEKREFTET_SENDT timestamp(6);

