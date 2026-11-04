drop table motedeltaker;

create table if not exists motedeltaker
(
    aktivitet_id             bigint    not null,
    aktivitet_versjon        bigint    not null,
    roller                   text      not null,
    navn                     text      not null,
    created_at timestamp default current_timestamp not null,
    constraint motedeltaker_pk primary key (aktivitet_id, aktivitet_versjon, navn, roller),
    constraint motedeltaker_mote_fk foreign key (aktivitet_id, aktivitet_versjon)
        references mote(aktivitet_id, versjon) on delete cascade
);
CREATE INDEX IF NOT EXISTS idx_motedeltaker_aktivitet_id ON motedeltaker(aktivitet_id);
CREATE INDEX IF NOT EXISTS idx_motedeltaker_aktivitet_id ON motedeltaker(aktivitet_id);
