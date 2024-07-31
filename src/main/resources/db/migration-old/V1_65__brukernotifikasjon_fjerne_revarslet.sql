update BRUKERNOTIFIKASJON set REVARSLET = null;
commit;
alter table BRUKERNOTIFIKASJON drop column REVARSLET;
