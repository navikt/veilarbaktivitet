update GJELDENDE_MOTE_SMS sms set KANAL = (
    select KANAL from MOTE
        inner join AKTIVITET A on A.AKTIVITET_ID = MOTE.AKTIVITET_ID and A.VERSJON = MOTE.VERSJON
        where GJELDENDE = 1
        and A.AKTIVITET_ID = sms.AKTIVITET_ID
    );
