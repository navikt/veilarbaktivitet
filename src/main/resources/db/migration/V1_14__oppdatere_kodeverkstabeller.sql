UPDATE AKTIVITET_TYPE AKTITYPE SET ENDRET_DATO = (SELECT OPPRETTET_DATO FROM AKTIVITET_TYPE WHERE AKTIVITET_TYPE_KODE = AKTITYPE.AKTIVITET_TYPE_KODE);
UPDATE AKTIVITET_TYPE AKTITYPE SET ENDRET_AV = (SELECT OPPRETTET_AV FROM AKTIVITET_TYPE WHERE AKTIVITET_TYPE_KODE = AKTITYPE.AKTIVITET_TYPE_KODE);

ALTER TABLE AKTIVITET_TYPE MODIFY ENDRET_DATO TIMESTAMP(6) NOT NULL;
ALTER TABLE AKTIVITET_TYPE MODIFY ENDRET_AV NVARCHAR2(255) NOT NULL;

UPDATE AKTIVITET_LIVSLOPSTATUS_TYPE LIVTYPE SET ENDRET_DATO = (SELECT OPPRETTET_DATO FROM AKTIVITET_LIVSLOPSTATUS_TYPE WHERE AKTIVITET_LIVSLOP_KODE = LIVTYPE.AKTIVITET_LIVSLOP_KODE);
UPDATE AKTIVITET_LIVSLOPSTATUS_TYPE LIVTYPE SET ENDRET_AV = (SELECT OPPRETTET_AV FROM AKTIVITET_LIVSLOPSTATUS_TYPE WHERE AKTIVITET_LIVSLOP_KODE = LIVTYPE.AKTIVITET_LIVSLOP_KODE);

ALTER TABLE AKTIVITET_LIVSLOPSTATUS_TYPE MODIFY ENDRET_DATO TIMESTAMP(6) NOT NULL;
ALTER TABLE AKTIVITET_LIVSLOPSTATUS_TYPE MODIFY ENDRET_AV NVARCHAR2(255) NOT NULL;