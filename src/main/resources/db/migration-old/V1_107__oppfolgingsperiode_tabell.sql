create table oppfolgingsperiode (
    id char(36) not null primary key,
    aktorId char(20) not null,
    fra timestamp not null,
    til timestamp
);

create index oppfolgingsperiode_aktorId_ix on oppfolgingsperiode (aktorId);