alter table STILLING_FRA_NAV add SOKNADSSTATUS varchar(255);

INSERT INTO TRANSAKSJONS_TYPE (TRANSAKSJONS_TYPE_KODE, OPPRETTET_DATO, OPPRETTET_AV)
VALUES ('SOKNADSSTATUS_ENDRET', CURRENT_TIMESTAMP, 'KASSERT');