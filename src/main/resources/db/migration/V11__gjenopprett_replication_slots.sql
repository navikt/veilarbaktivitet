DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM pg_replication_slots WHERE slot_name = 'ds_replication'
        ) THEN
            PERFORM PG_CREATE_LOGICAL_REPLICATION_SLOT('ds_replication', 'pgoutput');
        END IF;
    END;
$$;