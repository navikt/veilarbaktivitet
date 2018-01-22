-- Create a table that holds cached KVP information
-- from the veilarboppfolging application.
CREATE TABLE KVP (
  SERIAL                NUMBER         NOT NULL
    CONSTRAINT KVP_PK
    PRIMARY KEY,
  KVP_ID                NUMBER         NOT NULL,
  AKTOR_ID              VARCHAR2(20)   NOT NULL,
  ENHET                 NVARCHAR2(255) NOT NULL,
  OPPRETTET_DATO        TIMESTAMP(6)   NOT NULL,
  AVSLUTTET_DATO        TIMESTAMP(6)
);

-- KVP table indices.
CREATE INDEX KVP_ID_INDEX ON KVP (KVP_ID);
CREATE INDEX KVP_AKTORID_INDEX ON KVP (AKTOR_ID);

-- Add a column to the activity table storing the office ID.
--
-- If this field is not NULL, the information in the row should be
-- limited to only the users belonging to this specific unit.
ALTER TABLE AKTIVITET ADD KONTORSPERRE_ENHET_ID NVARCHAR2(255) NULL;

-- Insert coin to undo changes
--DROP TABLE KVP;
--ALTER TABLE AKTIVITET DROP COLUMN KONTORSPERRE_ENHET_ID;