DECLARE
 GEN_DIALOG_ID integer;
 AKTOER_ID_RANDOM integer;
 GEN_HENVENDELSE_ID integer;

BEGIN
  FOR i IN 1..5000000 LOOP
    SELECT dbms_random.value(0, 100000000) into AKTOER_ID_RANDOM from dual;

    select DIALOG_ID_SEQ.nextval into GEN_DIALOG_ID from dual;
    INSERT INTO DIALOG(dialog_id, aktor_id, overskrift,
    lest_av_bruker_tid,
    lest_av_veileder_tid,
    aktivitet_id,
    siste_Status_endring,
    siste_vente_pa_svar_tid,
    siste_ferdigbehandlet_tid,
    historisk,
    opprettet_dato,
    siste_ubehandlet_tid)
    VALUES (GEN_DIALOG_ID, AKTOER_ID_RANDOM, 'Generert-data-ytelsestest',
    sysdate,
    sysdate,
    null,
    sysdate,
    sysdate,
    sysdate,
    0,
    sysdate,
    sysdate);

   FOR i IN 1..5 LOOP
      select HENVENDELSE_ID_SEQ.nextval into GEN_HENVENDELSE_ID from dual;
      INSERT INTO HENVENDELSE(dialog_id, SENDT, AVSENDER_TYPE, AVSENDER_ID, TEKST, HENVENDELSE_ID)
      VALUES (GEN_DIALOG_ID, SYSDATE, 'BRUKER', AKTOER_ID_RANDOM, 'Generert-data-ytelsestest', GEN_HENVENDELSE_ID);
   END LOOP;

    COMMIT;
  END LOOP;
End;

