alter table oppfolgingsperiode
    add created timestamp default current_timestamp not null;
alter table oppfolgingsperiode
    add updated timestamp default current_timestamp not null;
