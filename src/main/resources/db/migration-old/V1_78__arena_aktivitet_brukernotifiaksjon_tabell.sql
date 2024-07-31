create table arena_aktivitet_brukernotifikasjon
(
    arena_aktivitet_id varchar2(255) not null,
    brukernotifikasjon_id number(19) not null unique references  BRUKERNOTIFIKASJON(id)
);


alter table BRUKERNOTIFIKASJON modify (AKTIVITET_ID NULL);
alter table BRUKERNOTIFIKASJON modify (OPPRETTET_PAA_AKTIVITET_VERSION NULL);