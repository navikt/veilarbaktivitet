create table motedeltaker
(
    aktivitet_id   bigint    not null,
    versjon        bigint    not null,
    deltaker_id    uuid      not null,
    rolle          text      not null,
    navn           text      not null,
    opprettet timestamp default current_timestamp not null,
    constraint motedeltaker_pk primary key (aktivitet_id, versjon, deltaker_id),
    constraint motedeltaker_mote_fk foreign key (aktivitet_id, versjon)
        references mote (aktivitet_id, versjon) on delete cascade
);

alter table motedeltaker owner to veilarbaktivitet;