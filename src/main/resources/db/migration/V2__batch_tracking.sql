CREATE TABLE IF NOT EXISTS batch_tracking(
    batchname varchar(60) primary key,
    last_offset bigint
)

