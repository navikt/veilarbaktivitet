CREATE TABLE ID_MAPPINGER
(
    AKTIVITET_ID         NUMBER(19),
    FUNKSJONELL_ID       VARCHAR(40),
    EKSTERN_REFERANSE_ID VARCHAR(40),
    SOURCE               NVARCHAR2(255),
    CONSTRAINT ID_MAPPINGER_PK PRIMARY KEY (FUNKSJONELL_ID)
);

