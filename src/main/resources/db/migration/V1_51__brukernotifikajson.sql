alter table BRUKERNOTIFIKASJON
    alter column SENDT rename to OPPRETTET;
alter table BRUKERNOTIFIKASJON
    add column SENDT timestamp;