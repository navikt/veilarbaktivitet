alter table aktivitetJobb add maks_id number(19);
update AKTIVITETJOBB set AKTIVITETJOBB.maks_id = (select max(AKTIVITET_ID) from aktivitet);