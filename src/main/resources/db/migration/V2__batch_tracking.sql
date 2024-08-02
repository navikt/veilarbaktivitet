CREATE TABLE IF NOT EXISTS batch_tracking(
    batch_name varchar(60) primary key,
    last_offset bigint
)

