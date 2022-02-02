alter table BRUKERNOTIFIKASJON add column smsTekst nvarchar2(160);
alter table BRUKERNOTIFIKASJON add column epostTittel nvarchar2(200);
alter table BRUKERNOTIFIKASJON add column epostBody varchar(10000);
