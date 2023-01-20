curl \
-H "test_ident: møtemannen" \
-H "test_ident_type: INTERN" \
-H 'Content-Type: application/json' \
-d '{
        "status": "PLANLAGT",
        "type": "MOTE",
        "tittel": "møte",
        "dato": "2023-01-20T09:53:25.000+01:00",
        "klokkeslett": "12:00",
        "varighet": "00:45",
        "kanal": "OPPMOTE",
        "adresse": "Oslo",
        "beskrivelse": "Vi ønsker å snakke med deg om aktiviteter du har gjennomført og videre oppfølging.",
        "forberedelser": null,
        "fraDato": "2023-01-20T11:00:00.000Z",
        "tilDato": "2023-01-20T11:45:00.000Z",
        "avtalt": false
    }' \
"http://localhost:8080/veilarbaktivitet/api/aktivitet/ny?fnr=23456789012"