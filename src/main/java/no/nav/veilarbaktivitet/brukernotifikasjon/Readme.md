# Bestillling av varsler gjennom brukernotifikasjon
Bruk [BrukernotifikasjonService](BrukernotifikasjonService.java) til og bestille og stoppe brukernotifikajoner async.  
Kafkameldinger for å Opprete og avlutte brukernotifkajonene produseres av cronjobber i undermodulene.

## brukernotifiaksjon docks:
### Brukernotifikasjon

[doks for brukernotifikasjon](https://navikt.github.io/brukernotifikasjon-docs/)   
[slack kanal #brukernotifikasjoner](https://nav-it.slack.com/archives/CR61BPH7G)

### EksternvarselKvitering
[doks for eksternvarsel kvitering](https://confluence.adeo.no/display/BOA/For+Konsumenter)  
[slck kanal #team_dokumentløsninger](https://nav-it.slack.com/archives/C6W9E5GPJ)

## Intern modell
### Varsel staus:
![Intern varsel staus](sendVarselStatus.svg)

### Varsel kvitering status:
![VarselKvitering status](VarselKviteringStatus.svg)