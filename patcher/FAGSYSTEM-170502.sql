-- Problemet er at for noen aktiviteter finnes det flere versjoner som er gjeldende.
-- Dette er et resultat av race-conditions i insertAktivitet. Forhåpentligvis vil disse ikke oppstå etter at kafka er tatt i bruk for sett-til-historisk

alter session set current_schema=veilarbaktivitet;

-- finner alle aktiviteter som har mer enn en gjeldende versjon
select aktivitet_id, count(aktivitet_id), min(versjon) from aktivitet where gjeldende=1 group by aktivitet_id having count(aktivitet_id) > 1 order by 2 desc;

-- denne finner også alle aktiviteter som har mer enn en gjeldende versjon
select
       aktivitet_id,
       versjon
    from AKTIVITET a1 WHERE EXISTS(
                                         select 1
                                         from AKTIVITET a2
                                         where  a2.VERSJON > a1.VERSJON
                                           and a2.AKTIVITET_ID = a1.AKTIVITET_ID
                                           and a2.GJELDENDE = 1 and a1.gjeldende = 1
                                     );

-- denne vil fikse problemet for alle aktiviteter som har 2 gjeldende versjoner (dette er tilfellet for alle duplikater i prod)
-- dersom det finnes flere enn 2 duplikater kan den kjøres flere ganger til alle er håndtert.

-- Essensen i merge er
-- MERGE INTO <target> USING  <source> ON <match target med source>
-- + hva du skal gjøre når du får en match
-- (+ eventuelt hva du skal gjøre hvis du ikke får en match)

-- Target er selvfølgelig AKTIVITET
-- Source-viewet er alle aktiviteter som har mer enn en gjeldende versjon, inkludert den laveste gjeldende versjonen.
-- Vi matcher Target mot Source-viewet ved å matche på aktivitet_id og versjon
-- Når vi får en match, oppdaterere vi gjeldende til 0 (false)

MERGE INTO AKTIVITET a
    USING ( -- aktivitet_id og laveste versjon for gjeldende aktiviteter som har mer enn en gjeldende aktivitet
        select aktivitet_id,
               count(aktivitet_id),
               min(versjon) as laveste_versjon
        from aktivitet where gjeldende=1 group by aktivitet_id having count(aktivitet_id) > 1) duplikater
    ON (a.aktivitet_id = duplikater.aktivitet_id and a.versjon = duplikater.laveste_versjon)
    WHEN MATCHED THEN
        UPDATE SET a.gjeldende = 0;

-- Dette er en annen variant som bruker UPDATE og EXISTS
-- subqueries i exists vil gi et resultat så lenge det finnes aktiviteter (a2) som har en nyere versjon enn den i 'hovedquerien' (a1) som også har gjeldende = 1.
-- Derfor vil også  hovedquerien oppdatere den laveste versjonen.
-- resultatet i subquerien blir ikke returnert, så det spiller ingen rolle hva man selecter på.
-- En fordel med denne updaten, er at den vil oppdatere så lenge det finnes flere gjeldende aktiviteter for en aktivitet_id, så den vil også håndtere flere enn to duplikater.
UPDATE AKTIVITET a1 SET GJELDENDE = 0 where gjeldende = 1 and exists(
        select 1
        from AKTIVITET a2
        where  a2.VERSJON > a1.VERSJON
          and a2.AKTIVITET_ID = a1.AKTIVITET_ID
          and a2.GJELDENDE = 1
    );

-- spørringene i toppen skal returnere null rader etter at patch er gjennomført.
-- oppdateringene kan kjøres flere ganger ut at det skjer noe mer enn det skal.

