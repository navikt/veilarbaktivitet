DECLARE
 AKTIVITET_ID integer;
 AKTOER_ID_RANDOM integer;

BEGIN
  FOR i IN 1..100000
    SELECT dbms_random.value(0, 100000000) into AKTOER_ID_RANDOM from dual;

    select AKTIVITET_ID_SEQ.nextval into AKTIVITET_ID from dual;
    INSERT INTO AKTIVITET (aktivitet_id, versjon, aktor_id, aktivitet_type_kode,fra_dato, til_dato, tittel, beskrivelse, livslopstatus_kode,avsluttet_kommentar, opprettet_dato, endret_dato, endret_av, lagt_inn_av, lenke, avtalt, gjeldende, transaksjons_type, historisk_dato)
    VALUES (aktivitet_id,1,AKTOER_ID_RANDOM,'EGENAKTIVITET',sysdate,sysdate,'GEN-DATA','','PLANLAGT','',sysdate,sysdate,'SYSTEM','','',0,1,'OPPRETTET',null);
    INSERT INTO EGENAKTIVITET(aktivitet_id, versjon, hensikt, oppfolging) values (aktivitet_id, 1, null, null);

    select AKTIVITET_ID_SEQ.nextval into AKTIVITET_ID from dual;
    INSERT INTO AKTIVITET (aktivitet_id, versjon, aktor_id, aktivitet_type_kode,fra_dato, til_dato, tittel, beskrivelse, livslopstatus_kode,avsluttet_kommentar, opprettet_dato, endret_dato, endret_av, lagt_inn_av, lenke, avtalt, gjeldende, transaksjons_type, historisk_dato)
    VALUES (aktivitet_id,1,AKTOER_ID_RANDOM,'IJOBB',sysdate,sysdate,'GEN-DATA','','PLANLAGT','',sysdate,sysdate,'SYSTEM','','',0,1,'OPPRETTET',null);
    INSERT INTO IJOBB(aktivitet_id, versjon, jobb_status, ansettelsesforhold, arbeidstid) values (aktivitet_id, 1, 'HELTID', null, null);

    select AKTIVITET_ID_SEQ.nextval into AKTIVITET_ID from dual;
    INSERT INTO AKTIVITET (aktivitet_id, versjon, aktor_id, aktivitet_type_kode,fra_dato, til_dato, tittel, beskrivelse, livslopstatus_kode,avsluttet_kommentar, opprettet_dato, endret_dato, endret_av, lagt_inn_av, lenke, avtalt, gjeldende, transaksjons_type, historisk_dato)
    VALUES (aktivitet_id,1,AKTOER_ID_RANDOM,'BEHANDLING',sysdate,sysdate,'GEN-DATA','','PLANLAGT','',sysdate,sysdate,'SYSTEM','','',0,1,'OPPRETTET',null);
    INSERT INTO BEHANDLING(aktivitet_id, versjon, behandling_sted, effekt, behandling_oppfolging, behandling_type) values (aktivitet_id, 1, null, null, null, null);

    select AKTIVITET_ID_SEQ.nextval into AKTIVITET_ID from dual;
    INSERT INTO AKTIVITET (aktivitet_id, versjon, aktor_id, aktivitet_type_kode,fra_dato, til_dato, tittel, beskrivelse, livslopstatus_kode,avsluttet_kommentar, opprettet_dato, endret_dato, endret_av, lagt_inn_av, lenke, avtalt, gjeldende, transaksjons_type, historisk_dato)
    VALUES (aktivitet_id,1,AKTOER_ID_RANDOM,'MOTE',sysdate,sysdate,'GEN-DATA','','PLANLAGT','',sysdate,sysdate,'SYSTEM','','',0,1,'OPPRETTET',null);
    INSERT INTO MOTE(aktivitet_id, versjon, adresse, forberedelser, kanal, referat, referat_publisert) values (aktivitet_id, 1, null, null, 'TELEFON', '', 0);

    select AKTIVITET_ID_SEQ.nextval into AKTIVITET_ID from dual;
    INSERT INTO AKTIVITET (aktivitet_id, versjon, aktor_id, aktivitet_type_kode,fra_dato, til_dato, tittel, beskrivelse, livslopstatus_kode,avsluttet_kommentar, opprettet_dato, endret_dato, endret_av, lagt_inn_av, lenke, avtalt, gjeldende, transaksjons_type, historisk_dato)
    VALUES (aktivitet_id,1,AKTOER_ID_RANDOM,'SOKEAVTALE',sysdate,sysdate,'GEN-DATA','','PLANLAGT','',sysdate,sysdate,'SYSTEM','','',0,1,'OPPRETTET',null);
    INSERT INTO SOKEAVTALE(aktivitet_id, versjon, antall_stillinger_sokes, avtale_oppfolging) values (aktivitet_id, 1, 1, null);

    select AKTIVITET_ID_SEQ.nextval into AKTIVITET_ID from dual;
    INSERT INTO AKTIVITET (aktivitet_id, versjon, aktor_id, aktivitet_type_kode,fra_dato, til_dato, tittel, beskrivelse, livslopstatus_kode,avsluttet_kommentar, opprettet_dato, endret_dato, endret_av, lagt_inn_av, lenke, avtalt, gjeldende, transaksjons_type, historisk_dato)
    VALUES (aktivitet_id,1,AKTOER_ID_RANDOM,'JOBBSOEKING',sysdate,sysdate,'GEN-DATA','','PLANLAGT','',sysdate,sysdate,'SYSTEM','','',0,1,'OPPRETTET',null);
    INSERT INTO STILLINGSSOK(aktivitet_id, versjon, arbeidsgiver, stillingstittel, kontaktperson, etikett, arbeidssted) values (aktivitet_id, 1, '', 'Stillingstittel', 'nn', 'SOKNAD_SENDT', 'Oslo');
    COMMIT;
  END LOOP;
End;