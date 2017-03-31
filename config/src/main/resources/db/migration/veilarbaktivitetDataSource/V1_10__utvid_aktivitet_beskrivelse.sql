alter table AKTIVITET drop column beskrivelse;
alter table AKTIVITET add beskrivelse clob;
alter table AKTIVITET modify lenke varchar(2000);
