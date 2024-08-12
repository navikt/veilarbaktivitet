-- Ble lagd med concurrently i prod med det funker ikke så bra i testene
create unique index if not exists aktivitet_brukernotifikasjon_pk
    on aktivitet_brukernotifikasjon (aktivitet_id, brukernotifikasjon_id);
