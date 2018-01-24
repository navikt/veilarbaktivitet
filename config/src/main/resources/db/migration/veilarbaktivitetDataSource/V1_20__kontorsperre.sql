-- Add a column to the activity table storing the office ID.
--
-- If this field is not NULL, the information in the row should be
-- limited to only the users belonging to this specific unit.
ALTER TABLE AKTIVITET ADD KONTORSPERRE_ENHET_ID NVARCHAR2(255) NULL;

-- Insert coin to undo changes
--ALTER TABLE AKTIVITET DROP COLUMN KONTORSPERRE_ENHET_ID;