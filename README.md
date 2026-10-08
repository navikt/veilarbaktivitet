# Veilarbaktivitet
Backend applikasjon for aktivitetsplanen. Tilbyr REST tjenester for å hente/opprette/endre aktiviteter.

## Komme i gang

```sh
./gradlew test
```

### Tilgang og kontorsperre
- Aktiviteter som veileder ikke har tilgang til pga kontorsperre vises ikke for veileder
- Hvis bruker er kontorsperret og veileder ikke har tilgang enheten til bruker, har ikke veileder tilgang til å opprette aktiviteter på bruker

Tilgang til enheten til bruker sjekkes via [dab](https://github.com/navikt/dab) `AuthService` sin `harTilgangTilEnhet` som kjører policy-en `NavAnsattTilgangTilNavEnhetPolicyInput` mot [poao-tilgang](https://github.com/navikt/poao-tilgang).

[Policien](https://github.com/navikt/poao-tilgang/blob/main/core/src/main/kotlin/no/nav/poao_tilgang/core/policy/impl/NavAnsattTilgangTilNavEnhetPolicyImpl.kt) sjekker følgende:
- Hvis bruker har admin-rollen returer Permit
- Hvis bruker ikke har tilgang til modia-oppfolging `0000-GA-Modia-Oppfolging` (noen plasser er dette kalt "skrivetilgang"), returner Deny
- Hvis bruker har oppgitt enhet som en AD-gruppe på formen `0000-GA-ENHET_<enhetId>` returner Permit, hvis ikke returner Deny


## Kontakt og spørsmål
Opprett en issue i GitHub for eventuelle spørsmål. 

