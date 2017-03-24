update aktivitet set status='PLANLAGT' where status is null;
alter table AKTIVITET modify status varchar(255) not null;