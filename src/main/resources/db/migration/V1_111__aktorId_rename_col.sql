ALTER TABLE OPPFOLGINGSPERIODE MODIFY (aktorIdFiks VARCHAR2(20) NOT NULL);
ALTER TABLE OPPFOLGINGSPERIODE DROP COLUMN AKTORID;
ALTER TABLE OPPFOLGINGSPERIODE RENAME COLUMN aktorIdFiks TO AKTORID;