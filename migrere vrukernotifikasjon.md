# Migrering fra gammel til ny brukernotifikasjon
## Info


## Migrering
[bestill tilgang til dev](https://github.com/navikt/min-side-brukervarsel-topic-iac/blob/main/dev-gcp/aapen-brukervarsel-v1.yaml)

bytt `implementation("com.github.navikt:brukernotifikasjon-schemas:$schema_version")`  
med `implementation("no.nav.tms.varsel:kotlin-builder:$brukernotifikasjon_verion")`

bytte fra jitpack til navs github proxy `maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")`
eventuelt bare legg den til hvis du har noe annet som må bruke jitpack

[se builder versioner](https://github.com/navikt/tms-varsel-authority/packages/1975431)


oprett ny felles string/string kafka producer (gamle var avro/avro) og forskjellige per type.
på topiken `aapen-brukervarsel-v1`

