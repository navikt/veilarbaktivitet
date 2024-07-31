create table brukernotifikajson_kvitering_tabell
(
    BRUKERNOTIFIKASJON_ID varchar2(255) not null,
    status varchar2(255) not null,
    melding varchar2(255) not null,
    distribusjonId number,
    beskjed clob not null
);