create sequence brukernotifikajson_kvitering_tabell_rowid_seq
    increment by -1;

alter sequence brukernotifikajson_kvitering_tabell_rowid_seq owner to veilarbaktivitet;

create sequence slettede_aktiviteter_rowid_seq
    increment by -1;

alter sequence slettede_aktiviteter_rowid_seq owner to veilarbaktivitet;

create sequence aktivitet_id_seq
    start with 964724
    cache 20;

alter sequence aktivitet_id_seq owner to veilarbaktivitet;

create sequence aktivitet_versjon_seq
    start with 2832882
    cache 20;

alter sequence aktivitet_versjon_seq owner to veilarbaktivitet;

create sequence iseq$$_125685
    start with 8859
    cache 20;

alter sequence iseq$$_125685 owner to veilarbaktivitet;

create table aktivitet_livslopstatus_type
(
    aktivitet_livslop_kode varchar(255) not null
        constraint livslopstatus_type_pk
            primary key,
    opprettet_dato         timestamp(6) not null,
    opprettet_av           varchar(255) not null,
    endret_dato            timestamp(6) not null,
    endret_av              varchar(255) not null
);

alter table aktivitet_livslopstatus_type
    owner to veilarbaktivitet;

create table aktivitet_type
(
    aktivitet_type_kode varchar(255) not null
        constraint aktivitet_type_pk
            primary key,
    opprettet_dato      timestamp(6) not null,
    opprettet_av        varchar(255) not null,
    endret_dato         timestamp(6) not null,
    endret_av           varchar(255) not null
);

alter table aktivitet_type
    owner to veilarbaktivitet;

create table aktivitetskort_msg_id
(
    message_id     varchar(40) not null
        constraint sys_c0021724
            primary key,
    funksjonell_id varchar(40) not null,
    opprettet_dato timestamp(6) default (CURRENT_TIMESTAMP)::timestamp without time zone,
    action_result  varchar(30),
    reason         varchar(255)
);

alter table aktivitetskort_msg_id
    owner to veilarbaktivitet;

create table brukernotifikajson_kvitering_tabell
(
    brukernotifikasjon_id varchar(255)                                                                           not null,
    status                varchar(255)                                                                           not null,
    melding               varchar(1024)                                                                          not null,
    distribusjonid        numeric,
    beskjed               text                                                                                   not null,
    rowid                 numeric(33) default nextval('brukernotifikajson_kvitering_tabell_rowid_seq'::regclass) not null
        constraint veilarbaktivitet_brukernotifikajson_kvitering_tabell_pk_rowid
            primary key
);

alter table brukernotifikajson_kvitering_tabell
    owner to veilarbaktivitet;

alter sequence brukernotifikajson_kvitering_tabell_rowid_seq owned by brukernotifikajson_kvitering_tabell.rowid;

create table forhaandsorientering
(
    id                     char(36)     not null
        constraint sys_c0017218
            primary key,
    aktor_id               varchar(20)  not null,
    arenaaktivitet_id      varchar(255)
        constraint fho_arena_arena_id_unique
            unique,
    aktivitet_id           bigint,
    aktivitet_versjon      varchar(255),
    type                   varchar(50)  not null,
    tekst                  varchar(1000),
    opprettet_dato         timestamp(6) not null,
    opprettet_av           varchar(20)  not null,
    lest_dato              timestamp(6),
    lest_aktivitet_versjon varchar(255),
    varsel_id              char(36),
    varsel_skal_stoppes    timestamp(6),
    varsel_stoppet         timestamp(6),
    brukernotifikasjon     smallint
);

alter table forhaandsorientering
    owner to veilarbaktivitet;

create index fho_arena_id
    on forhaandsorientering ((arenaaktivitet_id::character varying));

create table gjeldende_mote_sms
(
    aktivitet_id       bigint             not null
        constraint gjeldende_mote_sms_pk
            primary key,
    motetid            timestamp(6)       not null,
    kanal              varchar(255),
    brukernotifikasjon smallint default 0 not null
);

alter table gjeldende_mote_sms
    owner to veilarbaktivitet;

create table id_mappinger
(
    aktivitet_id         bigint,
    funksjonell_id       varchar(40) not null
        constraint id_mappinger_pk
            primary key,
    ekstern_referanse_id varchar(40),
    source               varchar(255)
);

alter table id_mappinger
    owner to veilarbaktivitet;

create index id_mappinger_ekstern_referanse_idx
    on id_mappinger (ekstern_referanse_id);

create table jobb_status_type
(
    jobb_status_kode varchar(255) not null
        constraint jobb_status_pk
            primary key,
    opprettet_dato   timestamp(6) not null,
    opprettet_av     varchar(255) not null,
    endret_dato      timestamp(6),
    endret_av        varchar(255)
);

alter table jobb_status_type
    owner to veilarbaktivitet;

create table kanal_type
(
    kanal_type_kode varchar(255) not null
        constraint kanal_type_pk
            primary key,
    opprettet_dato  timestamp(6) not null,
    opprettet_av    varchar(255) not null,
    endret_dato     timestamp(6),
    endret_av       varchar(255)
);

alter table kanal_type
    owner to veilarbaktivitet;

create table kassert_aktivitet
(
    aktivitet_id bigint not null
        constraint sys_c0022736
            primary key,
    begrunnelse  varchar(255)
);

alter table kassert_aktivitet
    owner to veilarbaktivitet;

create table oppfolgingsperiode
(
    id      char(36)                                                              not null
        constraint sys_c0025289
            primary key,
    fra     timestamp(6)                                                          not null,
    til     timestamp(6),
    created timestamp(6) default (CURRENT_TIMESTAMP)::timestamp without time zone not null,
    updated timestamp(6) default (CURRENT_TIMESTAMP)::timestamp without time zone not null,
    aktorid varchar(20)                                                           not null
);

alter table oppfolgingsperiode
    owner to veilarbaktivitet;

create table shedlock
(
    name       varchar(64) not null
        constraint sys_c0010223
            primary key,
    lock_until timestamp(3),
    locked_at  timestamp(3),
    locked_by  varchar(255)
);

alter table shedlock
    owner to veilarbaktivitet;

create table siste_oppfolgingsperiode
(
    periode_uuid varchar(36)  not null
        constraint sys_c0019248
            unique,
    aktorid      varchar(20)  not null
        constraint sys_c0019247
            primary key,
    startdato    timestamp(6) not null,
    sluttdato    timestamp(6)
);

alter table siste_oppfolgingsperiode
    owner to veilarbaktivitet;

create table slettede_aktiviteter
(
    aktivitet_id bigint                                                                  not null,
    tidspunkt    timestamp(6)                                                            not null,
    rowid        numeric(33) default nextval('slettede_aktiviteter_rowid_seq'::regclass) not null
        constraint veilarbaktivitet_slettede_aktiviteter_pk_rowid
            primary key
);

alter table slettede_aktiviteter
    owner to veilarbaktivitet;

alter sequence slettede_aktiviteter_rowid_seq owned by slettede_aktiviteter.rowid;

create index slettet_dato_aktivitet_idx
    on slettede_aktiviteter (tidspunkt);

create table stillingssok_etikett_type
(
    etikett_kode   varchar(255) not null
        constraint etikett_type_pk
            primary key,
    opprettet_dato timestamp(6) not null,
    opprettet_av   varchar(255) not null,
    endret_dato    timestamp(6),
    endret_av      varchar(255)
);

alter table stillingssok_etikett_type
    owner to veilarbaktivitet;

create table transaksjons_type
(
    transaksjons_type_kode varchar(255) not null
        constraint transaksjons_type_pk
            primary key,
    opprettet_dato         timestamp(6) not null,
    opprettet_av           varchar(255) not null,
    endret_dato            timestamp(6),
    endret_av              varchar(255)
);

alter table transaksjons_type
    owner to veilarbaktivitet;

create table aktivitet
(
    aktivitet_id                  bigint             not null,
    versjon                       bigint   default 0 not null,
    transaksjons_type             varchar(255)       not null
        constraint transaksjon_type_fk
            references transaksjons_type,
    aktor_id                      varchar(255),
    tittel                        varchar(255),
    aktivitet_type_kode           varchar(255)       not null
        constraint aktivitet_type_fk
            references aktivitet_type,
    avsluttet_kommentar           varchar(1000),
    lagt_inn_av                   varchar(255),
    fra_dato                      timestamp(6),
    til_dato                      timestamp(6),
    lenke                         text,
    opprettet_dato                timestamp(6)       not null,
    endret_dato                   timestamp(6)       not null,
    endret_av                     varchar(255),
    livslopstatus_kode            varchar(255)       not null
        constraint livslopstatus_type_fk
            references aktivitet_livslopstatus_type,
    beskrivelse                   text,
    avtalt                        smallint default 0 not null,
    historisk_dato                timestamp(6),
    gjeldende                     smallint default 0 not null,
    kontorsperre_enhet_id         varchar(255),
    lest_av_bruker_forste_gang    timestamp(6),
    automatisk_opprettet          smallint default 0 not null,
    mal_id                        varchar(255),
    portefolje_kafka_offset       bigint,
    fho_type                      varchar(255),
    fho_tekst                     varchar(1000),
    fho_lest                      timestamp(6),
    fho_id                        char(36)
        constraint aktivitet_fho_id_fk
            references forhaandsorientering,
    portefolje_kafka_offset_aiven bigint,
    oppfolgingsperiode_uuid       varchar(40),
    funksjonell_id                varchar(40),
    constraint aktivitet_pk
        primary key (aktivitet_id, versjon)
);

alter table aktivitet
    owner to veilarbaktivitet;

create index akt_aktorid_oppfolgingsperiode_idx
    on aktivitet (oppfolgingsperiode_uuid, (aktor_id::character varying));

create index aktivitet_aktor_idx
    on aktivitet ((aktor_id::character varying), (gjeldende::numeric(1, 0)));

create index aktivitet_fho_lest_idx
    on aktivitet ((aktivitet_id::numeric(19, 0)), (versjon::numeric(19, 0)), (gjeldende::numeric(1, 0)), fho_lest);

create index aktivitet_funksjonell_id_idx
    on aktivitet (funksjonell_id);

create index aktivitet_id_idx
    on aktivitet ((aktivitet_id::numeric(19, 0)), (gjeldende::numeric(1, 0)));

create index aktivitet_oppfolgingsperiode_idx
    on aktivitet ((1));

create index aktivitet_version_aiven_idx
    on aktivitet ((versjon::numeric(19, 0)), (portefolje_kafka_offset_aiven::numeric(19, 0)));

create index aktivitet_version_idx
    on aktivitet ((versjon::numeric(19, 0)));

create index aktivtet_smsquery_idx
    on aktivitet ((gjeldende::numeric(1, 0)), fra_dato, (livslopstatus_kode::character varying),
                  (aktivitet_type_kode::character varying));

create index endret_dato_aktivitet_idx
    on aktivitet (endret_dato);

create index historisk_idx
    on aktivitet (historisk_dato);

create index portefolje_kafka_index
    on aktivitet ((versjon::numeric(19, 0)), (portefolje_kafka_offset::numeric(19, 0)));

create table behandling
(
    aktivitet_id          bigint           not null,
    versjon               bigint default 0 not null,
    behandling_sted       varchar(255),
    effekt                varchar(255),
    behandling_oppfolging varchar(255),
    behandling_type       varchar(255),
    constraint behandling_pk
        primary key (aktivitet_id, versjon),
    constraint behandling_fk
        foreign key (aktivitet_id, versjon) references aktivitet
);

alter table behandling
    owner to veilarbaktivitet;

create table egenaktivitet
(
    aktivitet_id bigint           not null,
    versjon      bigint default 0 not null,
    hensikt      varchar(255),
    oppfolging   varchar(255),
    constraint egenaktivitet_pk
        primary key (aktivitet_id, versjon),
    constraint egenaktivitet_fk
        foreign key (aktivitet_id, versjon) references aktivitet
);

alter table egenaktivitet
    owner to veilarbaktivitet;

create table eksternaktivitet
(
    aktivitet_id             bigint             not null,
    versjon                  bigint   default 0 not null,
    source                   varchar(255),
    tiltak_kode              varchar(255),
    aktivitetkort_type       varchar(255),
    oppgave                  text,
    handlinger               text,
    detaljer                 text,
    etiketter                text,
    arena_id                 varchar(16),
    opprettet_som_historisk  smallint default 0,
    oppfolgingsperiode_slutt timestamp(6),
    endret_tidspunkt_kilde   timestamp(6),
    constraint eksternaktivitet_pk
        primary key (aktivitet_id, versjon),
    constraint eksternaktivitet_fk
        foreign key (aktivitet_id, versjon) references aktivitet
);

alter table eksternaktivitet
    owner to veilarbaktivitet;

create table ijobb
(
    aktivitet_id       bigint           not null,
    versjon            bigint default 0 not null,
    jobb_status        varchar(255)     not null
        constraint jobb_status_type_fk
            references jobb_status_type,
    ansettelsesforhold varchar(255),
    arbeidstid         varchar(255),
    constraint ijobb_pk
        primary key (aktivitet_id, versjon),
    constraint ijobb_fk
        foreign key (aktivitet_id, versjon) references aktivitet
);

alter table ijobb
    owner to veilarbaktivitet;

create table mote
(
    aktivitet_id      bigint             not null,
    versjon           bigint   default 0 not null,
    adresse           varchar(255),
    forberedelser     text,
    kanal             varchar(255)       not null
        constraint kanal_type_fk
            references kanal_type,
    referat           text,
    referat_publisert smallint default 0 not null,
    constraint mote_pk
        primary key (aktivitet_id, versjon),
    constraint mote_fk
        foreign key (aktivitet_id, versjon) references aktivitet
);

alter table mote
    owner to veilarbaktivitet;

create table mote_sms_historikk
(
    aktivitet_id bigint       not null,
    versjon      bigint       not null,
    motetid      timestamp(6) not null,
    varsel_id    varchar(255) not null
        constraint sys_c0012864
            unique,
    sendt        timestamp(6) not null,
    kanal        varchar(255),
    constraint mote_sms_historikk_pk
        primary key (aktivitet_id, versjon),
    constraint msh_aktivitet_fk
        foreign key (aktivitet_id, versjon) references aktivitet
);

alter table mote_sms_historikk
    owner to veilarbaktivitet;

create table sokeavtale
(
    aktivitet_id             bigint           not null,
    versjon                  bigint default 0 not null,
    antall_stillinger_sokes  bigint,
    avtale_oppfolging        text,
    antall_stillinger_i_uken bigint,
    constraint sokeavtale_pk
        primary key (aktivitet_id, versjon),
    constraint sokeavtale_fk
        foreign key (aktivitet_id, versjon) references aktivitet
);

alter table sokeavtale
    owner to veilarbaktivitet;

create table stilling_fra_nav
(
    aktivitet_id             bigint                                       not null,
    versjon                  bigint                                       not null,
    cv_kan_deles             smallint,
    cv_kan_deles_tidspunkt   timestamp(6),
    cv_kan_deles_av          varchar(255),
    cv_kan_deles_av_type     varchar(255),
    soknadsfrist             varchar(255),
    svarfrist                timestamp(5),
    arbeidsgiver             varchar(255),
    bestillingsid            varchar(255)                                 not null,
    stillingsid              varchar(255)                                 not null,
    arbeidssted              varchar(255),
    varselid                 varchar(255),
    kontaktperson_navn       varchar(255),
    kontaktperson_tittel     varchar(255),
    kontaktperson_mobil      varchar(255),
    soknadsstatus            varchar(255),
    cv_kan_deles_avtalt_dato timestamp,
    livslopsstatus           varchar(255) default NULL::character varying not null,
    detaljer                 varchar(255),
    constraint stilling_fra_nav_pk
        primary key (aktivitet_id, versjon),
    constraint stilling_fra_nav_fk
        foreign key (aktivitet_id, versjon) references aktivitet
);

alter table stilling_fra_nav
    owner to veilarbaktivitet;

create index stilling_fra_nav_bestillingsid_idx
    on stilling_fra_nav (bestillingsid);

create table stillingssok
(
    aktivitet_id    bigint           not null,
    versjon         bigint default 0 not null,
    arbeidsgiver    varchar(255),
    stillingstittel varchar(255),
    kontaktperson   varchar(255),
    etikett         varchar(255)
        constraint etikett_type_fk
            references stillingssok_etikett_type,
    arbeidssted     varchar(255),
    constraint stillingssok_aktivitet_pk
        primary key (aktivitet_id, versjon),
    constraint stillingssok_aktivitet_fk
        foreign key (aktivitet_id, versjon) references aktivitet
);

alter table stillingssok
    owner to veilarbaktivitet;

create table brukernotifikasjon
(
    id                              bigint       default nextval('"iseq$$_125685"'::regclass) not null
        constraint sys_c0018093
            primary key,
    brukernotifikasjon_id           varchar(255)                                              not null,
    aktivitet_id                    bigint,
    opprettet_paa_aktivitet_version bigint,
    foedselsnummer                  varchar(255)                                              not null,
    oppfolgingsperiode              varchar(255)                                              not null,
    type                            varchar(255)                                              not null,
    status                          varchar(255)                                              not null,
    melding                         varchar(500)                                              not null,
    varsel_feilet                   timestamp(6),
    avsluttet                       timestamp(6),
    opprettet                       timestamp(6),
    bekreftet_sendt                 timestamp(6),
    forsokt_sendt                   timestamp(6),
    ferdig_behandlet                timestamp(6),
    varsel_kvittering_status        varchar(255) default NULL::character varying              not null,
    smstekst                        varchar(160),
    eposttittel                     varchar(200),
    epostbody                       varchar(3000),
    url                             varchar(255),
    constraint brukernotifikasjon_fk
        foreign key (aktivitet_id, opprettet_paa_aktivitet_version) references aktivitet
);

alter table brukernotifikasjon
    owner to veilarbaktivitet;

create table aktivitet_brukernotifikasjon
(
    aktivitet_id                    bigint not null,
    opprettet_paa_aktivitet_version bigint not null,
    brukernotifikasjon_id           bigint not null
        constraint sys_c0019895
            unique
        constraint sys_c0019896
            references brukernotifikasjon,
    constraint fk_aktivitet_brukernotifikasjon_opprettet
        foreign key (aktivitet_id, opprettet_paa_aktivitet_version) references aktivitet
);

alter table aktivitet_brukernotifikasjon
    owner to veilarbaktivitet;

create table arena_aktivitet_brukernotifikasjon
(
    arena_aktivitet_id    varchar(255) not null,
    brukernotifikasjon_id bigint       not null
        constraint sys_c0019965
            unique
        constraint sys_c0019966
            references brukernotifikasjon
);

alter table arena_aktivitet_brukernotifikasjon
    owner to veilarbaktivitet;

create index brukernotifikasjon_aktivitet_idx
    on brukernotifikasjon ((aktivitet_id::numeric(19, 0)));

create unique index brukernotifikasjon_bestillingsid_idx
    on brukernotifikasjon (brukernotifikasjon_id);

create index brukernotifikasjon_status_idx
    on brukernotifikasjon (status);

create view dvh_akt_livslopstatus_type (aktivitet_livslop_kode, opprettet_dato, opprettet_av, endret_dato, endret_av) as
SELECT aktivitet_livslopstatus_type.aktivitet_livslop_kode::character varying AS aktivitet_livslop_kode,
       aktivitet_livslopstatus_type.opprettet_dato,
       aktivitet_livslopstatus_type.opprettet_av::character varying           AS opprettet_av,
       aktivitet_livslopstatus_type.endret_dato,
       aktivitet_livslopstatus_type.endret_av::character varying              AS endret_av
FROM aktivitet_livslopstatus_type;

alter table dvh_akt_livslopstatus_type
    owner to veilarbaktivitet;

create view dvh_aktivitet
            (aktivitet_id, versjon, transaksjons_type, aktor_id, tittel, aktivitet_type_kode, avsluttet_kommentar,
             lagt_inn_av, fra_dato, til_dato, lenke, opprettet_dato, endret_dato, endret_av, livslopstatus_kode,
             beskrivelse, avtalt_flagg, historisk_dato, gjeldende_flagg, oppfolgingsperiode_uuid, automatisk_opprettet)
as
SELECT aktivitet.aktivitet_id::numeric(19, 0)           AS aktivitet_id,
       aktivitet.versjon::numeric(19, 0)                AS versjon,
       aktivitet.transaksjons_type::character varying   AS transaksjons_type,
       aktivitet.aktor_id::character varying            AS aktor_id,
       aktivitet.tittel::character varying              AS tittel,
       aktivitet.aktivitet_type_kode::character varying AS aktivitet_type_kode,
       aktivitet.avsluttet_kommentar::character varying AS avsluttet_kommentar,
       aktivitet.lagt_inn_av::character varying         AS lagt_inn_av,
       aktivitet.fra_dato,
       aktivitet.til_dato,
       aktivitet.lenke,
       aktivitet.opprettet_dato,
       aktivitet.endret_dato,
       aktivitet.endret_av::character varying           AS endret_av,
       aktivitet.livslopstatus_kode::character varying  AS livslopstatus_kode,
       aktivitet.beskrivelse,
       aktivitet.avtalt::numeric(1, 0)                  AS avtalt_flagg,
       aktivitet.historisk_dato,
       aktivitet.gjeldende::numeric(1, 0)               AS gjeldende_flagg,
       aktivitet.oppfolgingsperiode_uuid,
       aktivitet.automatisk_opprettet::numeric(1, 0)    AS automatisk_opprettet
FROM aktivitet
WHERE aktivitet.aktivitet_type_kode::text <> 'EKSTERNAKTIVITET'::text;

alter table dvh_aktivitet
    owner to veilarbaktivitet;

create view dvh_aktivitet_type (aktivitet_type_kode, opprettet_dato, opprettet_av, endret_dato, endret_av) as
SELECT aktivitet_type.aktivitet_type_kode::character varying AS aktivitet_type_kode,
       aktivitet_type.opprettet_dato,
       aktivitet_type.opprettet_av::character varying        AS opprettet_av,
       aktivitet_type.endret_dato,
       aktivitet_type.endret_av::character varying           AS endret_av
FROM aktivitet_type;

alter table dvh_aktivitet_type
    owner to veilarbaktivitet;

create view dvh_egenaktivitet(aktivitet_id, versjon, hensikt, oppfolging, endret_dato) as
SELECT egenaktivitet.aktivitet_id::numeric(19, 0)  AS aktivitet_id,
       egenaktivitet.versjon::numeric(19, 0)       AS versjon,
       egenaktivitet.hensikt::character varying    AS hensikt,
       egenaktivitet.oppfolging::character varying AS oppfolging,
       aktivitet.endret_dato
FROM egenaktivitet
         LEFT JOIN aktivitet ON egenaktivitet.aktivitet_id::numeric = aktivitet.aktivitet_id::numeric(19, 0) AND
                                egenaktivitet.versjon::numeric = aktivitet.versjon::numeric(19, 0);

alter table dvh_egenaktivitet
    owner to veilarbaktivitet;

create view dvh_ijobb(aktivitet_id, versjon, jobb_status, ansettelsesforhold, arbeidstid) as
SELECT ijobb.aktivitet_id::numeric(19, 0)          AS aktivitet_id,
       ijobb.versjon::numeric(19, 0)               AS versjon,
       ijobb.jobb_status::character varying        AS jobb_status,
       ijobb.ansettelsesforhold::character varying AS ansettelsesforhold,
       ijobb.arbeidstid::character varying         AS arbeidstid
FROM ijobb;

alter table dvh_ijobb
    owner to veilarbaktivitet;

create view dvh_jobb_status_type (jobb_status_kode, opprettet_dato, opprettet_av, endret_dato, endret_av) as
SELECT jobb_status_type.jobb_status_kode::character varying AS jobb_status_kode,
       jobb_status_type.opprettet_dato,
       jobb_status_type.opprettet_av::character varying     AS opprettet_av,
       jobb_status_type.endret_dato,
       jobb_status_type.endret_av::character varying        AS endret_av
FROM jobb_status_type;

alter table dvh_jobb_status_type
    owner to veilarbaktivitet;

create view dvh_kanal_type(kanal_type_kode, opprettet_dato, opprettet_av, endret_dato, endret_av) as
SELECT kanal_type.kanal_type_kode::character varying AS kanal_type_kode,
       kanal_type.opprettet_dato,
       kanal_type.opprettet_av::character varying    AS opprettet_av,
       kanal_type.endret_dato,
       kanal_type.endret_av::character varying       AS endret_av
FROM kanal_type;

alter table dvh_kanal_type
    owner to veilarbaktivitet;

create view dvh_mote (aktivitet_id, versjon, adresse, forberedelser, kanal, referat, referat_publisert) as
SELECT mote.aktivitet_id::numeric(19, 0)     AS aktivitet_id,
       mote.versjon::numeric(19, 0)          AS versjon,
       mote.adresse::character varying       AS adresse,
       mote.forberedelser,
       mote.kanal::character varying         AS kanal,
       mote.referat,
       mote.referat_publisert::numeric(1, 0) AS referat_publisert
FROM mote;

alter table dvh_mote
    owner to veilarbaktivitet;

create view dvh_slettede_aktiviteter(aktivitet_id, tidspunkt) as
SELECT slettede_aktiviteter.aktivitet_id::numeric(19, 0) AS aktivitet_id,
       slettede_aktiviteter.tidspunkt
FROM slettede_aktiviteter;

alter table dvh_slettede_aktiviteter
    owner to veilarbaktivitet;

create view dvh_sokeavtale_aktivitet (aktivitet_id, versjon, antall_stillinger_sokes, avtale_oppfolging, endret_dato) as
SELECT sokeavtale.aktivitet_id::numeric(19, 0)            AS aktivitet_id,
       sokeavtale.versjon::numeric(19, 0)                 AS versjon,
       sokeavtale.antall_stillinger_sokes::numeric(19, 0) AS antall_stillinger_sokes,
       sokeavtale.avtale_oppfolging,
       aktivitet.endret_dato
FROM sokeavtale
         LEFT JOIN aktivitet ON sokeavtale.aktivitet_id::numeric = aktivitet.aktivitet_id::numeric(19, 0) AND
                                sokeavtale.versjon::numeric = aktivitet.versjon::numeric(19, 0);

alter table dvh_sokeavtale_aktivitet
    owner to veilarbaktivitet;

create view dvh_stilling_fra_nav_aktivitet
            (aktivitet_id, versjon, cv_kan_deles, cv_kan_deles_tidspunkt, cv_kan_deles_av, cv_kan_deles_av_type,
             soknadsfrist, svarfrist, arbeidsgiver, bestillingsid, stillingsid, arbeidssted, varselid,
             kontaktperson_navn, kontaktperson_tittel, kontaktperson_mobil, soknadsstatus, cv_kan_deles_avtalt_dato,
             livslopsstatus)
as
SELECT stilling_fra_nav.aktivitet_id::numeric(19, 0)            AS aktivitet_id,
       stilling_fra_nav.versjon::numeric(19, 0)                 AS versjon,
       stilling_fra_nav.cv_kan_deles::numeric(1, 0)             AS cv_kan_deles,
       stilling_fra_nav.cv_kan_deles_tidspunkt,
       stilling_fra_nav.cv_kan_deles_av::character varying      AS cv_kan_deles_av,
       stilling_fra_nav.cv_kan_deles_av_type::character varying AS cv_kan_deles_av_type,
       stilling_fra_nav.soknadsfrist,
       stilling_fra_nav.svarfrist,
       stilling_fra_nav.arbeidsgiver,
       stilling_fra_nav.bestillingsid,
       stilling_fra_nav.stillingsid,
       stilling_fra_nav.arbeidssted,
       stilling_fra_nav.varselid,
       stilling_fra_nav.kontaktperson_navn,
       stilling_fra_nav.kontaktperson_tittel,
       stilling_fra_nav.kontaktperson_mobil,
       stilling_fra_nav.soknadsstatus,
       stilling_fra_nav.cv_kan_deles_avtalt_dato,
       stilling_fra_nav.livslopsstatus
FROM stilling_fra_nav;

alter table dvh_stilling_fra_nav_aktivitet
    owner to veilarbaktivitet;

create view dvh_stillingsok_aktivitet
            (aktivitet_id, versjon, arbeidsgiver, stillingstittel, kontaktperson, etikett, arbeidssted, endret_dato) as
SELECT stillingssok.aktivitet_id::numeric(19, 0)       AS aktivitet_id,
       stillingssok.versjon::numeric(19, 0)            AS versjon,
       stillingssok.arbeidsgiver::character varying    AS arbeidsgiver,
       stillingssok.stillingstittel::character varying AS stillingstittel,
       stillingssok.kontaktperson::character varying   AS kontaktperson,
       stillingssok.etikett::character varying         AS etikett,
       stillingssok.arbeidssted::character varying     AS arbeidssted,
       aktivitet.endret_dato
FROM stillingssok
         LEFT JOIN aktivitet ON stillingssok.aktivitet_id::numeric = aktivitet.aktivitet_id::numeric(19, 0) AND
                                stillingssok.versjon::numeric = aktivitet.versjon::numeric(19, 0);

alter table dvh_stillingsok_aktivitet
    owner to veilarbaktivitet;

create view dvh_stillingsok_etikett_type(etikett_kode, opprettet_dato, opprettet_av, endret_dato, endret_av) as
SELECT stillingssok_etikett_type.etikett_kode::character varying AS etikett_kode,
       stillingssok_etikett_type.opprettet_dato,
       stillingssok_etikett_type.opprettet_av::character varying AS opprettet_av,
       stillingssok_etikett_type.endret_dato,
       stillingssok_etikett_type.endret_av::character varying    AS endret_av
FROM stillingssok_etikett_type;

alter table dvh_stillingsok_etikett_type
    owner to veilarbaktivitet;

create view dvh_transaksjon_type (transaksjons_type_kode, opprettet_dato, opprettet_av, endret_dato, endret_av) as
SELECT transaksjons_type.transaksjons_type_kode::character varying AS transaksjons_type_kode,
       transaksjons_type.opprettet_dato,
       transaksjons_type.opprettet_av::character varying           AS opprettet_av,
       transaksjons_type.endret_dato,
       transaksjons_type.endret_av::character varying              AS endret_av
FROM transaksjons_type;

alter table dvh_transaksjon_type
    owner to veilarbaktivitet;

