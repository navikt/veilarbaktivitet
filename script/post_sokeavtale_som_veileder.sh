curl \
-H "test_ident: møtemannen" \
-H "test_ident_type: INTERN" \
-H 'Content-Type: application/json' \
-d '{
        "status": "PLANLAGT",
        "type": "SOKEAVTALE",
        "tittel": "Avtale om å søke jobber",
        "fraDato": "2018-04-25T10:25:43.818Z",
        "tilDato": "2018-07-25T10:25:43.818Z",
        "periodeValidering": null,
        "antallStillingerIUken": 55555,
        "antallStillingerSokes": null,
        "avtaleOppfolging": null,
        "beskrivelse": "Dette er testdata. Ingen skal søke så mange jobber."
    }' \
"http://localhost:8080/veilarbaktivitet/api/aktivitet/ny?fnr=23456789012"