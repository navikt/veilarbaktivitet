-- Ble lagd med concurrently i prod med det funker ikke så bra i testene
CREATE INDEX IF NOT EXISTS idx_eksternaktivitet_tiltak_kode ON eksternaktivitet(tiltak_kode);
