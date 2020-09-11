alter table GJELDENDE_MOTE_SMS add KANAL nvarchar2(255);
alter table MOTE_SMS_HISTORIKK add KANAL nvarchar2(255);

-- må kjøres etter vi har begynt og sette inn i kanal
-- update GJELDENDE_MOTE_SMS sms set KANAL = (
--     select KANAL from MOTE
--         inner join AKTIVITET A on A.AKTIVITET_ID = MOTE.AKTIVITET_ID and A.VERSJON = MOTE.VERSJON
--         where GJELDENDE = 1
--         and A.AKTIVITET_ID = sms.AKTIVITET_ID
--     );
