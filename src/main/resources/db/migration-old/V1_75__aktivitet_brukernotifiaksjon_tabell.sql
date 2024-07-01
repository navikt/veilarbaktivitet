create table aktivitet_brukernotifikasjon
(
    aktivitet_id  number(19) not null ,
    opprettet_paa_aktivitet_version number(19) not null ,
    avsluttet_paa_aktivitet_version number(19),
    brukernotifikasjon_id number(19) not null unique references  BRUKERNOTIFIKASJON(id),
    constraint  FK_aktivitet_brukernotifikasjon_opprettet foreign key (aktivitet_id, opprettet_paa_aktivitet_version) references  AKTIVITET(AKTIVITET_ID, VERSJON),
    constraint  FK_aktivitet_brukernotifikasjon_avsluttetet foreign key (aktivitet_id, avsluttet_paa_aktivitet_version) references  AKTIVITET(AKTIVITET_ID, VERSJON)
);

alter table BRUKERNOTIFIKASJON add url varchar2(255);