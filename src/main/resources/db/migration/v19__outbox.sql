create table outbox (
    id        bigserial  primary key,
    topic     text       not null,
    key       text       not null,
    payload   text       not null,
    opprettet timestamp  not null default current_timestamp
);

CREATE INDEX idx_outbox_topic
    ON outbox (topic);

CREATE INDEX idx_outbox_key
    ON outbox (key);
