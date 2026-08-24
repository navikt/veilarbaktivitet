-- Ble lagd med concurrently i prod med det funker ikke så bra i testene
CREATE INDEX IF NOT EXISTS idx_eksternaktivitet_aktivitetkort_type ON eksternaktivitet(aktivitetkort_type);
