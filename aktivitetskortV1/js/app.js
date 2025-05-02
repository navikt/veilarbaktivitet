
    const schema = {
  "asyncapi": "2.5.0",
  "info": {
    "title": "Aktivitetskort as a service - AkaaS",
    "description": "Data-drevet grensesnitt for å opprette og endre aktivitetskort i aktivitetsplanen.  \nLøsningen er beregnet på team som har data i sine systemer som skal representeres som en aktivitet i aktivitetsplan, for eksempel tiltaksgjennomføringer. \nEksisterende aktiviteter kan oppdateres ved å sende en ny versjon av aktivitetskortet med samme funksjonelle id.  \nTjenesten har støtte for en rekke dynamiske innholdskomponenter i aktivitetskortet, slik at produsentene på tjenesten har stor grad av kontroll på hvordan aktivitetskortet skal se ut.  \nDynamiske komponenter inkluderer 'oppgave', 'handlinger', 'detaljer' og 'etiketter'. Disse er beskrevet i skjemaet.  \n[Lenke til Mural](https://app.mural.co/t/navdesign3580/m/navdesign3580/1663573695869/1a7a897ac6b2af3fccc11aa65371a83d840e2020?wid=0-1665572511683&outline=open)\n",
    "version": "0.0.1",
    "contact": {
      "name": "Team Dab",
      "url": "https://nav-it.slack.com/archives/C04HS60F283"
    }
  },
  "defaultContentType": "application/json",
  "channels": {
    "aktivitetskort-v1.1": {
      "description": "Topic for å bestille eller oppdatere aktiviteter i aktivitetsplan",
      "bindings": {
        "kafka": {
          "key": {
            "type": "string",
            "format": "uuid",
            "description": "aktiviteskorts id. Også kalt funksjonell id"
          }
        }
      },
      "publish": {
        "message": {
          "oneOf": [
            {
              "name": "aktivitetskortBestilling",
              "title": "Aktivitetskort bestilling",
              "summary": "Bestilling for oppretting eller endring av et aktivitetskort",
              "description": "aktivitetskort.id må være det samme som kafka key",
              "traits": [
                {
                  "headers": {
                    "$schema": "https://json-schema.org/draft-07/schema",
                    "$id": "https://navikt.github.io/veilarbaktivitet/schemas/akaas/Aktivitetskort.V1.headers.schema.yml",
                    "type": "object",
                    "required": [
                      "Nav-Call-Id"
                    ],
                    "properties": {
                      "Nav-Call-Id": {
                        "type": "string",
                        "minLength": 1,
                        "description": "brukes for logging",
                        "x-parser-schema-id": "<anonymous-schema-56>"
                      }
                    }
                  }
                }
              ],
              "payload": {
                "$schema": "https://json-schema.org/draft-07/schema",
                "$id": "https://navikt.github.io/veilarbaktivitet/schemas/akaas/Aktivitetskort.V1.aktivitetskort.schema.yml",
                "required": [
                  "messageId",
                  "source",
                  "aktivitetskortType",
                  "aktivitetskort",
                  "actionType"
                ],
                "type": "object",
                "additionalProperties": false,
                "properties": {
                  "messageId": {
                    "type": "string",
                    "format": "uuid",
                    "description": "Unik id for denne meldingen brukes til deduplisering",
                    "x-parser-schema-id": "<anonymous-schema-2>"
                  },
                  "source": {
                    "type": "string",
                    "maxLength": 200,
                    "description": "Applikasjonen eller teamet som er avsender av meldingen.",
                    "enum": [
                      "ARENA_TILTAK_AKTIVITET_ACL",
                      "TEAM_TILTAK",
                      "TEAM_KOMET",
                      "REKRUTTERINGSBISTAND"
                    ],
                    "x-parser-schema-id": "<anonymous-schema-3>"
                  },
                  "aktivitetskortType": {
                    "type": "string",
                    "$id": "aktivitetskortType",
                    "enum": [
                      "MIDLERTIDIG_LONNSTILSKUDD",
                      "VARIG_LONNSTILSKUDD",
                      "ARBEIDSTRENING",
                      "INDOPPFAG",
                      "ARBFORB",
                      "AVKLARAG",
                      "VASV",
                      "ARBRRHDAG",
                      "DIGIOPPARB",
                      "JOBBK",
                      "GRUPPEAMO",
                      "GRUFAGYRKE",
                      "REKRUTTERINGSTREFF",
                      "ARENA_TILTAK"
                    ],
                    "description": "Aktivitetskort typer som er tillatt å opprette via tjenesten. Denne enumereringen vil utvides etterhvert.\nMIDLERTIDIG_LONNSTILSKUDD - Midlertidig lønnstilskudd (Team Tiltak)\nVARIG_LONNSTILSKUDD - Varig lønnstilskudd (Team Tiltak)\nARBEIDSTRENING - Arbeidstrening (Team Tiltak)\nINDOPPFAG - Oppfølging (Team Komet)\nARBFORB - Arbeidsforberedende trening - AFT (Team Komet)\nAVKLARAG  - Avklaring (Team Komet)\nVASV  - Varig tilrettelagt arbeid i skjermet virksomhet (Team Komet)\nARBRRHDAG” - Arbeidsrettet rehabilitering - dag (Team Komet)\nDIGIOPPARB - Digitalt jobbsøkerkurs for arbeidsledige - jobbklubb (Team Komet)\nJOBBK - Jobbklubb (Team Komet)\nGRUPPEAMO Gruppe AMO - arbeidsmarkedsopplæring (Team Komet)\nGRUFAGYRKE  - Gruppe Fag- og yrkesopplæring VGS og høyere yrkesfaglig utdanning (Team Komet)\nREKRUTTERINGSTREFF - Rekrutteringstreff mellom arbeidsgivere og potensielle arbeidstakere (Team Toi)\nARENA_TILTAK - Resten av arbeidsmarkedstiltakene i arena (>100)\n"
                  },
                  "actionType": {
                    "type": "string",
                    "enum": [
                      "UPSERT_AKTIVITETSKORT_V1",
                      "KASSER_AKTIVITET"
                    ],
                    "description": "Actiontype forteller hvordan meldingen skal behandles. Forløpig har vi kun støtte for å opprette/oppdatere (UPSERT) og kassere aktivitetskort.",
                    "x-parser-schema-id": "<anonymous-schema-4>"
                  },
                  "aktivitetskort": {
                    "additionalProperties": false,
                    "type": "object",
                    "required": [
                      "id",
                      "personIdent",
                      "tittel",
                      "aktivitetStatus",
                      "endretAv",
                      "endretTidspunkt",
                      "avtaltMedNav"
                    ],
                    "properties": {
                      "id": {
                        "type": "string",
                        "format": "uuid",
                        "description": "Funksjonell ID for aktiviteten er en globalt unik UUID for aktiviteten.\nVed vellykket opprettelse av aktiviteten, vil aktiviteten kunne gjenfinnnes\nved hjelp av denne iden. Senere modifisering av samme aktivitet vil også\nbruke denne iden for å identifisere korrekt aktivitet.\n",
                        "x-parser-schema-id": "<anonymous-schema-6>"
                      },
                      "personIdent": {
                        "type": "string",
                        "examples": [
                          "10068831950"
                        ],
                        "description": "Norsk identitetsnummer (d-nr eller f-nr) for personen som eier aktivitetskortet",
                        "x-parser-schema-id": "<anonymous-schema-7>"
                      },
                      "tittel": {
                        "type": "string",
                        "description": "Tittelen på aktivitetskortet",
                        "x-parser-schema-id": "<anonymous-schema-8>"
                      },
                      "aktivitetStatus": {
                        "type": "string",
                        "enum": [
                          "FORSLAG",
                          "PLANLAGT",
                          "GJENNOMFORES",
                          "FULLFORT",
                          "AVBRUTT"
                        ],
                        "description": "Dette feltet forteller hvilken status aktiviteten har, og dermed hvilken kolonne aktivitetskortet skal ligge i i aktivitetsplanen. Merk at aktivitetskort som er fullført eller avbrutt ikke kan endres i etterkant, da dette er en endelig status.",
                        "x-parser-schema-id": "<anonymous-schema-9>"
                      },
                      "startDato": {
                        "type": [
                          "string",
                          "null"
                        ],
                        "format": "date",
                        "examples": [
                          "2022-03-01"
                        ],
                        "description": "Planlagt startdato for aktiviteten",
                        "x-parser-schema-id": "<anonymous-schema-10>"
                      },
                      "sluttDato": {
                        "type": [
                          "string",
                          "null"
                        ],
                        "format": "date",
                        "examples": [
                          "2022-05-15"
                        ],
                        "description": "Planlagt sluttdato for aktiviteten",
                        "x-parser-schema-id": "<anonymous-schema-11>"
                      },
                      "beskrivelse": {
                        "type": [
                          "string",
                          "null"
                        ],
                        "description": "Beskrivende tekst for aktiviteten",
                        "x-parser-schema-id": "<anonymous-schema-12>"
                      },
                      "endretAv": {
                        "type": "object",
                        "additionalProperties": false,
                        "required": [
                          "ident",
                          "identType"
                        ],
                        "description": "Sporingsfelt som identifiserer hvem som oppretter eller endrer aktiviteten",
                        "properties": {
                          "ident": {
                            "type": "string",
                            "examples": [
                              "Z999999"
                            ],
                            "description": "Id til bruker som oppretter eller endrer aktiviteten.\nTiltaksarrangør og arbeidsgiver er orgNr.\nPersonbruker er norsk-ident (dnr eller fnr).\nNavident er ident til navansatt: feks Z999999.\nArenaident: kun til intern bruk.\nSystem: Systembruker\n",
                            "x-parser-schema-id": "<anonymous-schema-14>"
                          },
                          "identType": {
                            "type": "string",
                            "enum": [
                              "ARENAIDENT",
                              "NAVIDENT",
                              "PERSONBRUKER",
                              "TILTAKSARRAGOER",
                              "ARBEIDSGIVER",
                              "SYSTEM"
                            ],
                            "x-parser-schema-id": "<anonymous-schema-15>"
                          }
                        },
                        "examples": [
                          {
                            "ident": "AAA123",
                            "identType": "ARENAIDENT"
                          }
                        ],
                        "x-parser-schema-id": "<anonymous-schema-13>"
                      },
                      "endretTidspunkt": {
                        "type": "string",
                        "format": "date-time",
                        "examples": [
                          "2022-09-17T21:00:14+01:00"
                        ],
                        "description": "Dato-tid for opprettelse eller endring i kildesystemet i ISO8601 format (ZonedDateTime). Vær oppmerksom på at dersom du ikke oppgir sone, vil tidspunktet bli tolket som vår lokale tidssone, altså Europe/Oslo",
                        "x-parser-schema-id": "<anonymous-schema-16>"
                      },
                      "avtaltMedNav": {
                        "type": "boolean",
                        "description": "Hvorvidt aktiviteten skal bli markert som 'Avtalt med NAV'. Dette gjelder typisk aktiviteter med aktivitetsplikt.",
                        "x-parser-schema-id": "<anonymous-schema-17>"
                      },
                      "oppgave": {
                        "additionalProperties": false,
                        "type": [
                          "object",
                          "null"
                        ],
                        "properties": {
                          "ekstern": {
                            "additionalProperties": false,
                            "type": [
                              "object",
                              "null"
                            ],
                            "properties": {
                              "tekst": {
                                "type": "string",
                                "x-parser-schema-id": "<anonymous-schema-20>"
                              },
                              "subtekst": {
                                "type": "string",
                                "x-parser-schema-id": "<anonymous-schema-21>"
                              },
                              "url": {
                                "type": "string",
                                "format": "url",
                                "x-parser-schema-id": "<anonymous-schema-22>"
                              }
                            },
                            "description": "En oppgave vil bli rendret som et alert-panel med en lenke i aktivitetskortet. Dette signaliserer at det er en oppgave bruker eller veileder trenger å gjøre (f.eks. signere en avtale). Selve handlingen vil utføres i et annet system enn aktivitetsplan, og vil typisk resultere i en ny versjon av aktiviteten med oppdatert status sendes inn på denne tjenesten.",
                            "x-parser-schema-id": "<anonymous-schema-19>"
                          },
                          "intern": "$ref:$.channels.aktivitetskort-v1.1.publish.message.oneOf[0].payload.properties.aktivitetskort.properties.oppgave.properties.ekstern"
                        },
                        "x-parser-schema-id": "<anonymous-schema-18>"
                      },
                      "handlinger": {
                        "type": [
                          "array",
                          "null"
                        ],
                        "items": {
                          "type": "object",
                          "additionalProperties": false,
                          "properties": {
                            "tekst": {
                              "type": "string",
                              "x-parser-schema-id": "<anonymous-schema-25>"
                            },
                            "subtekst": {
                              "type": "string",
                              "x-parser-schema-id": "<anonymous-schema-26>"
                            },
                            "url": {
                              "type": "string",
                              "format": "url",
                              "x-parser-schema-id": "<anonymous-schema-27>"
                            },
                            "lenkeType": {
                              "type": "string",
                              "enum": [
                                "EKSTERN",
                                "INTERN",
                                "FELLES"
                              ],
                              "x-parser-schema-id": "<anonymous-schema-28>"
                            }
                          },
                          "x-parser-schema-id": "<anonymous-schema-24>"
                        },
                        "description": "Handlinger vil rendres som lenkeseksjoner i aktivitetskortet. Dette kan brukes for å tilby tilleggsfunksjonalitet i kildesystemet, f.eks. Les avtalen, Evaluer deltakelsen på tiltaket, o.l.",
                        "x-parser-schema-id": "<anonymous-schema-23>"
                      },
                      "detaljer": {
                        "type": "array",
                        "items": {
                          "type": "object",
                          "additionalProperties": false,
                          "properties": {
                            "label": {
                              "type": "string",
                              "x-parser-schema-id": "<anonymous-schema-31>"
                            },
                            "verdi": {
                              "type": "string",
                              "x-parser-schema-id": "<anonymous-schema-32>"
                            }
                          },
                          "x-parser-schema-id": "<anonymous-schema-30>"
                        },
                        "description": "For å vise selvdefinerte informasjonsfelter på aktivitetskortet. Disse rendres som enkle label/tekst komponenter i samme rekkefølge som de ligger i meldingen.",
                        "x-parser-schema-id": "<anonymous-schema-29>"
                      },
                      "etiketter": {
                        "type": "array",
                        "items": {
                          "type": "object",
                          "additionalProperties": false,
                          "properties": {
                            "tekst": {
                              "type": "string",
                              "maxLength": 20,
                              "x-parser-schema-id": "<anonymous-schema-35>"
                            },
                            "sentiment": {
                              "type": "string",
                              "enum": [
                                "POSITIVE",
                                "NEGATIVE",
                                "NEUTRAL"
                              ],
                              "x-parser-schema-id": "<anonymous-schema-36>"
                            },
                            "kode": {
                              "type": "string",
                              "x-parser-schema-id": "<anonymous-schema-37>"
                            }
                          },
                          "description": "Etiketter rendres som etiketter (Tags) på aktivitetskortet. Teksten som sendes inn vil vises som på etiketten med styling basert på sentimentet. Kode er valgfritt og blir foreløpig ikke brukt.",
                          "x-parser-schema-id": "<anonymous-schema-34>"
                        },
                        "x-parser-schema-id": "<anonymous-schema-33>"
                      }
                    },
                    "x-parser-schema-id": "<anonymous-schema-5>"
                  }
                },
                "definitions": {
                  "oppgave": "$ref:$.channels.aktivitetskort-v1.1.publish.message.oneOf[0].payload.properties.aktivitetskort.properties.oppgave.properties.ekstern",
                  "lenkeseksjon": "$ref:$.channels.aktivitetskort-v1.1.publish.message.oneOf[0].payload.properties.aktivitetskort.properties.handlinger.items",
                  "attributt": "$ref:$.channels.aktivitetskort-v1.1.publish.message.oneOf[0].payload.properties.aktivitetskort.properties.detaljer.items",
                  "tag": "$ref:$.channels.aktivitetskort-v1.1.publish.message.oneOf[0].payload.properties.aktivitetskort.properties.etiketter.items",
                  "aktivitetskort": "$ref:$.channels.aktivitetskort-v1.1.publish.message.oneOf[0].payload.properties.aktivitetskort"
                }
              },
              "headers": {
                "$schema": "https://json-schema.org/draft-07/schema",
                "$id": "https://navikt.github.io/veilarbaktivitet/schemas/akaas/Aktivitetskort.V1.headers.schema.yml",
                "type": "object",
                "required": "$ref:$.channels.aktivitetskort-v1.1.publish.message.oneOf[0].traits[0].headers.required",
                "properties": {
                  "Nav-Call-Id": {
                    "type": "string",
                    "minLength": 1,
                    "description": "brukes for logging",
                    "x-parser-schema-id": "<anonymous-schema-1>"
                  }
                }
              }
            },
            {
              "name": "aktivitetskortkassering",
              "title": "Aktivitetskort kassering",
              "summary": "Melding som overskriver alle versoner og historikk for aktiviteten",
              "description": "aktivitetsid må være det samme som kafka key",
              "traits": [
                "$ref:$.channels.aktivitetskort-v1.1.publish.message.oneOf[0].traits[0]"
              ],
              "payload": {
                "$schema": "https://json-schema.org/draft-07/schema",
                "$id": "https://navikt.github.io/veilarbaktivitet/schemas/akaas/Aktivitetskort.V1.kasser.schema.yml",
                "required": [
                  "source",
                  "actionType",
                  "aktivitetsId",
                  "personIdent",
                  "navIdent",
                  "messageId"
                ],
                "type": "object",
                "additionalProperties": false,
                "properties": {
                  "source": "$ref:$.channels.aktivitetskort-v1.1.publish.message.oneOf[0].payload.properties.source",
                  "sendt": {
                    "type": "string",
                    "format": "date-time",
                    "x-parser-schema-id": "<anonymous-schema-39>"
                  },
                  "actionType": {
                    "const": "KASSER_AKTIVITET",
                    "x-parser-schema-id": "<anonymous-schema-40>"
                  },
                  "aktivitetsId": "$ref:$.channels.aktivitetskort-v1.1.publish.message.oneOf[0].payload.properties.aktivitetskort.properties.id",
                  "personIdent": {
                    "type": "string",
                    "examples": [
                      "10068831950"
                    ],
                    "description": "Norsk identitetsnummer (d-nr eller f-nr) for personen som eier aktivitetskortet",
                    "x-parser-schema-id": "<anonymous-schema-41>"
                  },
                  "navIdent": {
                    "type": "string",
                    "examples": [
                      "Z999999"
                    ],
                    "description": "Id til bruker som kasserer aktivitetskortet",
                    "x-parser-schema-id": "<anonymous-schema-42>"
                  },
                  "begrunnelse": {
                    "type": "string",
                    "description": "begrunnelse for kasseing som er synlig for bruker inne i aktivitetskortet",
                    "x-parser-schema-id": "<anonymous-schema-43>"
                  },
                  "messageId": {
                    "type": "string",
                    "format": "uuid",
                    "description": "Unik id for denne meldingen brukes til deduplisering",
                    "x-parser-schema-id": "<anonymous-schema-44>"
                  }
                }
              },
              "headers": {
                "$schema": "https://json-schema.org/draft-07/schema",
                "$id": "https://navikt.github.io/veilarbaktivitet/schemas/akaas/Aktivitetskort.V1.headers.schema.yml",
                "type": "object",
                "required": "$ref:$.channels.aktivitetskort-v1.1.publish.message.oneOf[0].traits[0].headers.required",
                "properties": {
                  "Nav-Call-Id": {
                    "type": "string",
                    "minLength": 1,
                    "description": "brukes for logging",
                    "x-parser-schema-id": "<anonymous-schema-38>"
                  }
                }
              }
            }
          ]
        }
      }
    },
    "aktivitetskort-feil-v1": {
      "description": "Dead-letter kø for aktivitetskort meldinger som ikke kan behandles.",
      "bindings": {
        "kafka": {
          "key": {
            "type": "string",
            "format": "uuid",
            "description": "aktiviteskorts id. Også kalt funksjonell id"
          }
        }
      },
      "subscribe": {
        "message": {
          "name": "aktivitetskortFeilMelding",
          "title": "Aktivitetskort feilmelding",
          "summary": "Feilmelding for aktivitetskort som ikke kan behandles",
          "traits": [
            "$ref:$.channels.aktivitetskort-v1.1.publish.message.oneOf[0].traits[0]"
          ],
          "payload": {
            "$schema": "https://json-schema.org/draft-07/schema",
            "$id": "https://navikt.github.io/veilarbaktivitet/schemas/akaas/Aktivitetskort.V1.aktivitetskortfeilmelding.schema.yml",
            "required": [
              "key",
              "timestamp",
              "failingMessage",
              "errorMessage",
              "errorType",
              "source"
            ],
            "type": "object",
            "additionalProperties": false,
            "properties": {
              "key": "$ref:$.channels.aktivitetskort-v1.1.publish.message.oneOf[0].payload.properties.aktivitetskort.properties.id",
              "source": "$ref:$.channels.aktivitetskort-v1.1.publish.message.oneOf[0].payload.properties.source",
              "timestamp": {
                "type": "string",
                "format": "date-time",
                "x-parser-schema-id": "<anonymous-schema-46>"
              },
              "failingMessage": {
                "type": "string",
                "description": "Hele payloaden til den feilende meldingen",
                "x-parser-schema-id": "<anonymous-schema-47>"
              },
              "errorMessage": {
                "type": "string",
                "description": "Feilmelding",
                "examples": [
                  "DuplikatMeldingFeil Melding allerede handtert, ignorer",
                  "DeserialiseringsFeil Meldingspayload er ikke gyldig json",
                  "ManglerOppfolgingsperiodeFeil Finner ingen passende oppfølgingsperiode for aktivitetskortet."
                ],
                "x-parser-schema-id": "<anonymous-schema-48>"
              },
              "errorType": {
                "description": "Alle mulige funksjonelle-feil som kan oppstå under prosessering av aktivitetskort-meldinger og kasserings-meldinger",
                "type": "string",
                "enum": [
                  "AKTIVITET_IKKE_FUNNET",
                  "DESERIALISERINGSFEIL",
                  "DUPLIKATMELDINGFEIL",
                  "KAFKA_KEY_ULIK_AKTIVITETSID",
                  "MANGLER_OPPFOLGINGSPERIODE",
                  "MESSAGEID_LIK_AKTIVITETSID",
                  "UGYLDIG_IDENT",
                  "ULOVLIG_ENDRING"
                ],
                "x-parser-schema-id": "<anonymous-schema-49>"
              }
            },
            "definitions": {
              "failingMessage": {
                "type": "string",
                "description": "Denne inneholder hele originalmeldingen som ble sendt på aktivitetskort-v1. Formatet er json.",
                "x-parser-schema-id": "<anonymous-schema-50>"
              }
            }
          },
          "headers": {
            "$schema": "https://json-schema.org/draft-07/schema",
            "$id": "https://navikt.github.io/veilarbaktivitet/schemas/akaas/Aktivitetskort.V1.headers.schema.yml",
            "type": "object",
            "required": "$ref:$.channels.aktivitetskort-v1.1.publish.message.oneOf[0].traits[0].headers.required",
            "properties": {
              "Nav-Call-Id": {
                "type": "string",
                "minLength": 1,
                "description": "brukes for logging",
                "x-parser-schema-id": "<anonymous-schema-45>"
              }
            }
          }
        }
      }
    },
    "aktivitetskort-kvittering-v1": {
      "description": "Kvitteringskø for aktivitetskort meldinger.",
      "bindings": {
        "kafka": {
          "key": {
            "type": "string",
            "format": "uuid",
            "description": "aktiviteskorts id. Også kalt funksjonell id"
          }
        }
      },
      "subscribe": {
        "message": {
          "name": "aktivitetskortkvittering",
          "title": "Aktivitetskort kvittering",
          "summary": "Kvitteringsmelding for aktivitetskortBestilling",
          "traits": [
            "$ref:$.channels.aktivitetskort-v1.1.publish.message.oneOf[0].traits[0]"
          ],
          "payload": {
            "$schema": "https://json-schema.org/draft-07/schema",
            "$id": "https://navikt.github.io/veilarbaktivitet/schemas/akaas/Aktivitetskort.V1.aktivitetskortkvittering.schema.yml",
            "required": [
              "aktivitetId",
              "behandlingstype",
              "suksess",
              "behandlingsStatus",
              "behandlet"
            ],
            "type": "object",
            "properties": {
              "aktivitetId": "$ref:$.channels.aktivitetskort-v1.1.publish.message.oneOf[0].payload.properties.aktivitetskort.properties.id",
              "meldingId": {
                "type": "string",
                "format": "uuid",
                "description": "Samme meldingsid som aktivitetskort-bestillingen",
                "x-parser-schema-id": "<anonymous-schema-52>"
              },
              "behandlingstype": "$ref:$.channels.aktivitetskort-v1.1.publish.message.oneOf[0].payload.properties.actionType",
              "suksess": {
                "type": "boolean",
                "description": "Sier om behandling av meldingen var vellykket. Dersom 'false' vil behandlingsStatus og feilmelding gi ytterligere informasjon om feilen.",
                "x-parser-schema-id": "<anonymous-schema-53>"
              },
              "behandlingsStatus": "$ref:$.channels.aktivitetskort-feil-v1.subscribe.message.payload.properties.errorType",
              "feilmelding": {
                "type": "string",
                "description": "I tilfelle feil, inneholder exception.message.",
                "x-parser-schema-id": "<anonymous-schema-54>"
              },
              "behandlet": {
                "type": "string",
                "format": "date-time",
                "examples": [
                  "2022-09-17T21:00:14"
                ],
                "description": "Dato-tid for behandling av meldingen",
                "x-parser-schema-id": "<anonymous-schema-55>"
              }
            }
          },
          "headers": {
            "$schema": "https://json-schema.org/draft-07/schema",
            "$id": "https://navikt.github.io/veilarbaktivitet/schemas/akaas/Aktivitetskort.V1.headers.schema.yml",
            "type": "object",
            "required": "$ref:$.channels.aktivitetskort-v1.1.publish.message.oneOf[0].traits[0].headers.required",
            "properties": {
              "Nav-Call-Id": {
                "type": "string",
                "minLength": 1,
                "description": "brukes for logging",
                "x-parser-schema-id": "<anonymous-schema-51>"
              }
            }
          }
        }
      }
    }
  },
  "components": {
    "messages": {
      "aktivitetskort": "$ref:$.channels.aktivitetskort-v1.1.publish.message.oneOf[0]",
      "kassering": "$ref:$.channels.aktivitetskort-v1.1.publish.message.oneOf[1]",
      "aktivitetskortfeilmelding": "$ref:$.channels.aktivitetskort-feil-v1.subscribe.message",
      "aktivitetskortkvittering": "$ref:$.channels.aktivitetskort-kvittering-v1.subscribe.message"
    },
    "messageTraits": {
      "commonHeaders": "$ref:$.channels.aktivitetskort-v1.1.publish.message.oneOf[0].traits[0]"
    }
  },
  "x-parser-spec-parsed": true,
  "x-parser-api-version": 3,
  "x-parser-spec-stringified": true
};
    const config = {"show":{"sidebar":true},"sidebar":{"showOperations":"byDefault"}};
    const appRoot = document.getElementById('root');
    AsyncApiStandalone.render(
        { schema, config, }, appRoot
    );
  