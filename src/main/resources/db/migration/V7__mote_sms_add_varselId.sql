ALTER TABLE GJELDENDE_MOTE_SMS
ADD VARSEL_ID uuid;
ALTER TABLE GJELDENDE_MOTE_SMS
ADD CREATED_AT timestamp default current_timestamp;