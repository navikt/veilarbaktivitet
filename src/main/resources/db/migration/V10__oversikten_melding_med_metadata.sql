CREATE TYPE OVERSIKTEN_UTSENDING_STATUS AS ENUM ('SKAL_SENDES', 'SENDT', 'SKAL_IKKE_SENDES');
CREATE TYPE OVERSIKTEN_OPERASJON AS ENUM ('START', 'OPPDATER', 'STOPP');
CREATE TYPE OVERSIKTEN_KATEGORI AS ENUM ('UDELT_SAMTALEREFERAT ');

create table oversikten_melding_med_metadata
(
    id               SERIAL PRIMARY KEY,
    melding_key      uuid                        not null,
    fnr              varchar(11)                 not null,
    opprettet        timestamp                   not null,
    tidspunkt_sendt  timestamp,
    utsending_status OVERSIKTEN_UTSENDING_STATUS not null,
    melding          json                        not null,
    kategori         OVERSIKTEN_KATEGORI         not null,
    operasjon        OVERSIKTEN_OPERASJON        not null
);
create index oversikten_melding_med_metadata_melding_key_pk on oversikten_melding_med_metadata (melding_key);
create index oversikten_melding_med_metadata_utsending_status_idx on oversikten_melding_med_metadata (utsending_status);

create function melding_key_eksisterer(key uuid) returns boolean as
$$
select exists (select from oversikten_melding_med_metadata where melding_key = key);
$$ language sql;

create function aktivitet_id_eksisterer(akt_id bigint) returns boolean as
$$
select exists (select from aktivitet where aktivitet_id = akt_id);
$$ language sql;


create table oversikten_melding_aktivitet_mapping
(
    oversikten_melding_key uuid                not null check (melding_key_eksisterer(oversikten_melding_key)),
    aktivitet_id           bigint              not null check (aktivitet_id_eksisterer(aktivitet_id)),
    kategori               oversikten_kategori not null,
    PRIMARY KEY (aktivitet_id, kategori)
);
CREATE INDEX oversikten_melding_aktivitet_mapping_melding_key ON oversikten_melding_aktivitet_mapping (oversikten_melding_key);
CREATE INDEX oversikten_melding_aktivitet_mapping_aktivitet_id ON oversikten_melding_aktivitet_mapping (aktivitet_id);
