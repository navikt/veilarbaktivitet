ALTER TABLE aktivitetskort_msg_id
    ADD COLUMN offset integer default NULL,
    ADD COLUMN partition smallint NULL;
