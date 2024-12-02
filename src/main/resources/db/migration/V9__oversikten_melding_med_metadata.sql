create table oversikten_melding_med_metadata (
    melding_key                  uuid            not null,
    fnr                          varchar(11)     not null,
    opprettet                    timestamp       not null,
    tidspunkt_sendt              timestamp,
    utsending_status             text            not null,
    melding                      json            not null,
    kategori                     text            not null
);
create index oversikten_melding_med_metadata_melding_key_idx on oversikten_melding_med_metadata(melding_key);