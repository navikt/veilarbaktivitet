alter table ARENA_FORHAANDSORIENTERING
    add column lest timestamp;

create index arena_fho_lest_idx on ARENA_FORHAANDSORIENTERING (lest, arenaaktivitet_id);

alter table AKTIVITET
    add column fho_lest timestamp;

create index aktivitet_fho_lest_idx on AKTIVITET (AKTIVITET_ID, VERSJON, GJELDENDE, fho_lest);
