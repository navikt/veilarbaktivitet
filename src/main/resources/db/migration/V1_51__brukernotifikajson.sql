alter table BRUKERNOTIFIKASJON
    rename column SENDT to OPPRETTET;
alter table BRUKERNOTIFIKASJON
    add column BEKREFTET_SENDT timestamp;