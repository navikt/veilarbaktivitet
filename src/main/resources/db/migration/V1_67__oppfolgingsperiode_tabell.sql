create table siste_oppfolgingspeiode (
    periode_uuid char(36) not null unique,
    aktorId char(20) not null primary key,
    startdato timestamp not null,
    sluttdato timestamp
);