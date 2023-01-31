# Testbruker for Modia Arbeidsrettet oppfølging
aktivitetsplan og arbeidsrettet-dialog
## Test veileder

Lag en veileder  gjennom [IDA](http://ida.intern.nav.no/) med rettighetene:  
•	0000-GA-BD06_ModiaGenerellTilgang  
•	0000-GA-Modia-Oppfolging  
•	0000-GA-GOSYS_OPPGAVE_BEHANDLER  
•	0000-GA-GOSYS_NASJONAL  
•	0000-GA-GOSYS_UTVIDBAR_TIL_NASJONAL

Hvis veileder i tillegg skal ha spesialtilganger må du ha disse:  
•	0000-GA-Aa-register-Lese (rett til å registrere som arbeidssøker)  
•	0000-GA-GOSYS_KODE6  
•	0000-GA-GOSYS_KODE7  
•	0000-GA-GOSYS_SENSITIVT (egen ansatt)

Veileder må i tillegg ha tilgang til NAV-Enhet brukere tilhører.  
Leggs til i Axsys i IDA. Fagområde OPP(følging)  
Logg inn i [Modia arbeidsrettet oppfølging](https://veilarbportefoljeflate.dev.intern.nav.no/) med Trygdeetaten-ident til IDA veilederen.


## Test bruker
Gå til [Dolly](https://dolly.ekstern.dev.nav.no/testnorge)   
[Brukerdokumentasjon for doly](https://navikt.github.io/testnorge/applications/dolly/)


søke etter bruker i testnorge med
* Alder 18-67 år
* Adresse som veilederen din har tilgang til
    * F.eks adresse, og postnr. 8300 Svolvær, da vil din veileder ha tilgang til disse på NAV enhet 1860 Lofoten.


under importering må du velge:
* Arbeidsytelser
    * 11.5 vedtak
* Kontakt og reservasjonsregistret
    * du kan bare sende fåråndsårentering på brukere som er registerert i krr og har epost eller mobil
    * du kan **ikke** sende fåråndsårentering med  standard kontakt- og reservasjonsopplysninger


Testmiljø Q1 og Q2.

Log inn i [aktivitesplan](https://aktivitetsplan.dev.nav.no) med testbrukeren din.
