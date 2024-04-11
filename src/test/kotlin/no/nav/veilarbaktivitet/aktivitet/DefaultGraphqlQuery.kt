package no.nav.veilarbaktivitet.aktivitet

val dollar = '$'
object DefaultGraphqlQuery {
    val query: String = """
        query(${dollar}fnr: String!) {
        perioder(fnr: ${dollar}fnr) {
            id,
            aktiviteter {
                id,
                funksjonellId,
                versjon,
                tittel,
                beskrivelse,
                lenke,
                type,
                status,
                fraDato,
                tilDato,
                opprettetDato,
                endretDato,
                endretAv,
                historisk,
                avsluttetKommentar,
                avtalt,
                forhaandsorientering {
                    id,
                    type,
                    tekst,
                    lestDato,
                }
                endretAvType,
                transaksjonsType,
                malid,
                oppfolgingsperiodeId,

                etikett,
                kontaktperson,
                arbeidsgiver,
                arbeidssted,
                stillingsTittel,

                hensikt,
                oppfolging,

                antallStillingerSokes,
                antallStillingerIUken,
                avtaleOppfolging,

                jobbStatus,
                ansettelsesforhold,
                arbeidstid,

                behandlingType,
                behandlingSted,
                effekt,
                behandlingOppfolging,

                adresse,
                forberedelser,
                kanal,
                referat,
                erReferatPublisert,

                stillingFraNavData {
                    cvKanDelesData {
                        kanDeles,
                        endretTidspunkt,
                        endretAv,
                        endretAvType,
                        avtaltDato,
                    }
                    soknadsfrist,
                    svarfrist,
                    arbeidsgiver,
                    bestillingsId,
                    stillingsId,
                    arbeidssted,
                    kontaktpersonData {
                        navn,
                        tittel,
                        mobil,
                    }
                    soknadsstatus,
                    livslopsStatus,
                    varselId,
                    detaljer,
                }

                eksternAktivitet {
                    type,
                    oppgave {
                        ekstern {
                            subtekst,
                            tekst,
                            url
                        }
                        intern {
                            subtekst,
                            tekst,
                            url
                        }
                    }
                    handlinger {
                        url,
                        tekst,
                        subtekst,
                        lenkeType
                    }
                    detaljer {
                        label,
                        verdi
                    }
                    etiketter {
                        tekst,
                        kode,
                        sentiment
                    }
                }
            },
        }
    }
    """.trimIndent().replace("\n", "")
}