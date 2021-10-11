alter table STILLING_FRA_NAV
    add LIVSLOPSSTATUS varchar(255) not null default 'HAR_SVART';

update STILLING_FRA_NAV
set LIVSLOPSSTATUS='PROVER_VARSLING'
where CV_KAN_DELES is null;

alter table STILLING_FRA_NAV
    ALTER COLUMN LIVSLOPSSTATUS varchar(255) default (null) not null;