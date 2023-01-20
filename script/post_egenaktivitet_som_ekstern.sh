curl \
-H "test_ident: planleggersen" \
-H "test_ident_type: EKSTERN" \
-H 'Content-Type: application/json' \
-d '{"status":"PLANLAGT",
      "type":"EGEN",
      "tittel":"Lage aktivitet",
      "fraDato":"2023-01-16T15:39:03.000+01:00",
      "tilDato":"2023-01-19T15:39:22.000+01:00",
      "periodeValidering":null,
      "hensikt":"Teste backend",
      "beskrivelse":null,
      "oppfolging":null,
      "lenke":null}' \
"http://localhost:8080/veilarbaktivitet/api/aktivitet/ny"