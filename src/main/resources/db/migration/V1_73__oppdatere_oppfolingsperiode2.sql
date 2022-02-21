alter table aktivitetJobb add column maks_id number(19);
update AKTIVITETJOBB set AKTIVITETJOBB.maks_id = (select max(AKTIVITETJOBB.AKTIVITET_ID) from aktivitet);