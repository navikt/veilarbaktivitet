package no.nav.veilarbaktivitet.aktivitetskort

import lombok.extern.slf4j.Slf4j
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.AktivitetMuterbareFelter
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.Eksternaktivitet

@Slf4j
object AktivitetskortCompareUtil {
    fun erFaktiskOppdatert(innkommende: Eksternaktivitet.Endre, eksisterende: AktivitetData): Boolean {
        val eksisterendeSomEndre = Eksternaktivitet.Endre(
            id = eksisterende.id,
            versjon = eksisterende.versjon,
            muterbareFelter = AktivitetMuterbareFelter(
                tittel = eksisterende.tittel,
                beskrivelse = eksisterende.beskrivelse,
                fraDato = eksisterende.fraDato,
                tilDato = eksisterende.tilDato,
                lenke = eksisterende.lenke,
            ),
            sporing = innkommende.sporing, // Ikke sammenlignet
            eksternAktivitetData = eksisterende.eksternAktivitetData,
            erAvtalt = eksisterende.isAvtalt, // Ikke sammenlignet - håndteres i eget kall
            status = eksisterende.status // Ikke sammenlignet
        )

        // Sammenlign bare muterbare felter og ekstern aktivitet data
        val muterbareFelterEndret =
            innkommende.muterbareFelter.tittel != eksisterendeSomEndre.muterbareFelter.tittel
                || innkommende.muterbareFelter.beskrivelse != eksisterendeSomEndre.muterbareFelter.beskrivelse
                || innkommende.muterbareFelter.fraDato != eksisterendeSomEndre.muterbareFelter.fraDato
                || innkommende.muterbareFelter.tilDato != eksisterendeSomEndre.muterbareFelter.tilDato
                || innkommende.muterbareFelter.lenke != eksisterendeSomEndre.muterbareFelter.lenke

        val eksternDataEndret = innkommende.eksternAktivitetData != eksisterendeSomEndre.eksternAktivitetData

        return muterbareFelterEndret || eksternDataEndret
    }
}
