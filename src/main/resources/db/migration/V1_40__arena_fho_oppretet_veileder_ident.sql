alter table ARENA_FORHAANDSORIENTERING add oppretet_av_ident nvarchar2(255) not null default 'ikke-satt';

alter table ARENA_FORHAANDSORIENTERING alter column oppretet_av_ident drop default;
