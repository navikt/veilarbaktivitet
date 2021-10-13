-- View for aktivitettype 'STILLING_FRA_NAV'
CREATE VIEW DVH_STILLING_FRA_NAV_AKTIVITET AS(
                                             SELECT AKTIVITET_ID,
                                                    VERSJON,
                                                    CV_KAN_DELES,             -- 0 = kan ikke deles, 1 = kan deles
                                                    CV_KAN_DELES_TIDSPUNKT,   -- TIMESTAMP - når svar er gitt
                                                    CV_KAN_DELES_AV,
                                                    CV_KAN_DELES_AV_TYPE,     -- ["NAV", "BRUKER"]
                                                    SOKNADSFRIST,
                                                    SVARFRIST,                -- TIMESTAMP - Svarfrist for bruker
                                                    ARBEIDSGIVER,
                                                    BESTILLINGSID,
                                                    STILLINGSID,              -- Annonseid
                                                    ARBEIDSSTED,
                                                    VARSELID,
                                                    KONTAKTPERSON_NAVN,
                                                    KONTAKTPERSON_TITTEL,
                                                    KONTAKTPERSON_MOBIL,
                                                    SOKNADSSTATUS,
                                                    CV_KAN_DELES_AVTALT_DATO, -- DATE - Dato for da veileder snakket med bruker hvis de svarer på vegne av bruker
                                                    LIVSLOPSSTATUS
                                             FROM
        STILLING_FRA_NAV
);