
select * from mote;

create unique index if not exists mote_publisert_index
    on mote (referat_publisert);