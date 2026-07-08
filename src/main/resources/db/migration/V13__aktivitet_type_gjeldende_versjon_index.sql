create index if not exists aktivitet_type_gjeldende_versjon_idx
    on aktivitet (aktivitet_type_kode, gjeldende, versjon);
