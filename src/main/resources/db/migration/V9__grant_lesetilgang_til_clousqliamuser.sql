DO
$$
    BEGIN
        IF (SELECT exists(SELECT rolname FROM pg_roles WHERE rolname = 'cloudsqliamuser'))
        THEN
            GRANT USAGE ON SCHEMA veilarbaktivitet to "cloudsqliamuser";
            GRANT SELECT ON ALL TABLES IN SCHEMA veilarbaktivitet TO "cloudsqliamuser";
            ALTER DEFAULT PRIVILEGES IN SCHEMA veilarbaktivitet GRANT SELECT ON TABLES TO "cloudsqliamuser";
        END IF;
    END
$$;
