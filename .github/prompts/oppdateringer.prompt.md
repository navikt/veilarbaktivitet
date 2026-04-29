---
name: ukentlige-oppdateringer
description: Kjør ukentlige Dependabot-oppdateringer for dette Gradle/Kotlin-repoet og klargjør for merge til main
---

Du er en agent som utfører ukentlige avhengighetsoppdateringer for dette repoet. Dependabot har allerede åpnet PRer — din jobb er å samle dem på én branch, løse eventuelle problemer underveis, og klargjøre for menneskelig gjennomgang og merge.


## Kontekst om repoet

- **Byggsystem**: Gradle (`./gradlew`)
- **Språk**: Kotlin / Java
- **Test**: `./gradlew test`
- **Bygg**: `./gradlew build`
- **CI/CD**: Deploy til dev skjer via `workflow_dispatch` — ikke nødvendig å deploye som en del av dette oppsettet


## Steg 1 — Finn ukenummer og opprett branch

```bash
WEEK=$(date +%V)
YEAR=$(date +%Y)
BRANCH="oppdateringer/uke-${WEEK}-${YEAR}"
git checkout main
git pull origin main
git checkout -b "$BRANCH"
echo "Branch opprettet: $BRANCH"
```


## Steg 2 — Hent og verifiser åpne Dependabot-PRer

```bash
gh pr list --author "app/dependabot" --state open --json number,title,headRefName,labels,author
```

### ⚠️ Sikkerhetssjekk før merge

**Verifiser for hver PR at:**
1. `author.login` er nøyaktig `app/dependabot` — ikke bare at tittelen ser riktig ut
2. Branch-navnet følger mønsteret `dependabot/<ecosystem>/<pakke>`
3. Endringer er begrenset til `build.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `settings.gradle.kts` eller workflow-filer

```bash
gh pr view <nr> --json author,headRefName,files \
  | jq '{author: .author.login, branch: .headRefName, files: [.files[].path]}'
```

**Ikke merge PRer som:**
- Har en annen avsender enn `app/dependabot`
- Inneholder endringer i andre filer enn forventet

### ⚠️ Migreringsguide ved major-versjonshopp — ALLTID før merge

Dersom en PR bumper et rammeverk til en **ny major-versjon** (f.eks. Spring Boot 3.x → 4.x, Kotlin 1.x → 2.x):
**hent og les migreringsguiden** før du gjør tilpasninger i kodebasen.

```bash
# Eksempel: Spring Boot 4.0 migreringsguide
curl -sL "https://raw.githubusercontent.com/wiki/spring-projects/spring-boot/Spring-Boot-4.0-Migration-Guide.md" \
  | head -200

# Eksempel: Spring Framework 7.0
curl -sL "https://raw.githubusercontent.com/wiki/spring-projects/spring-framework/Upgrading-to-Spring-Framework-7.0.md" \
  | head -200
```

| Rammeverk | Migreringsguide-URL |
|-----------|---------------------|
| Spring Boot | `https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-<NY_MAJOR>-Migration-Guide` |
| Spring Framework | `https://github.com/spring-projects/spring-framework/wiki/Upgrading-to-Spring-Framework-<NY_MAJOR>.<MINOR>` |
| Kotlin | `https://kotlinlang.org/docs/whatsnew<NY_MAJOR><MINOR>.html` |
| Gradle (wrapper) | `https://docs.gradle.org/<NY_VERSION>/userguide/upgrading_version_<GAMMEL_MAJOR>.html` |
| Hibernate ORM | `https://docs.jboss.org/hibernate/orm/<NY_MAJOR>.0/migration-guide/migration-guide.html` |

**Les spesielt etter:** `breaking`, `removed`, `renamed`, `requires`, `deprecated`, `migration guide`

> **Merk:** For Spring Boot bør du **også** sjekke release notes ved minor-bump (f.eks. 3.3 → 3.4),
> siden Spring har tradisjon for aggressiv deprecation mellom minor-versjoner.
> Kjør `./gradlew build` og sjekk for deprecation-advarsler i buildoutputen.

### ⚠️ Kjente koblinger — sjekk disse ved major-bump

| Pakke som bumpes (major) | Sjekk mot | Hvorfor |
|---|---|---|
| `spring-boot` | `spring-framework`, `hibernate-core`, Java-versjonskrav | Spring Boot major krever ofte nyere Spring Framework og Java |
| `kotlin` | Kotlin-features brukt i kodebasen, coroutines-versjon | Ny major kan innføre stricter type-checking |
| `gradle` (wrapper) | Alle Gradle-plugins | Plugin-API endres mellom major-versjoner |
| `mockito-core` | Test-syntaks i eksisterende tester | Major kan endre mocking-API |

### Prioriter sikkerhets-PRer

```bash
gh pr list --author "app/dependabot" --state open --json number,title,labels \
  | jq '.[] | select(.labels[].name | test("security"))'
```

Sikkerhets-PRer (label `security`) merges **før** ordinære oppdateringer.

### Sorteringsrekkefølge

| Prioritet | Kategori |
|-----------|----------|
| 1 | GitHub Actions (`github-actions` ecosystem) |
| 2 | Gradle wrapper (`gradle-wrapper.properties`) |
| 3 | Build-plugins (minor+patch, Dependabot-gruppe) |
| 4 | Alle andre biblioteker (minor+patch, Dependabot-gruppe) |
| 5 | Major-versjonshopp (individuell gjennomgang — les migreringsguide!) |


## Steg 3 — Merge PRer

For hver PR i sorteringsrekkefølgen:

```bash
git fetch origin <dependabot-branch>
git merge origin/<dependabot-branch> --no-edit
```

Kjør tester etter hver PR (eller etter en gruppert PR fra Dependabot):

```bash
./gradlew test
```

### Dersom `build.gradle.kts` / `libs.versions.toml` har merge-konflikter

```bash
# Behold den høyeste versjonen, fjern konfliktmarkørene manuelt
# Deretter verifiser at dependency-treet er konsistent:
./gradlew dependencies --configuration runtimeClasspath | head -80
./gradlew test
git add build.gradle.kts gradle/libs.versions.toml
git commit --amend --no-edit
```

### Dersom tester feiler

1. Les testoutputen nøye
2. Sjekk om feilen skyldes en breaking change (les PR-beskrivelsen og migreringsguiden)
3. Gjør nødvendige tilpasninger: `git add -A && git commit -m "fix: tilpass kode etter <pakkenavn>-oppdatering"`
4. Hvis feilen krever større refaktorering: reverter pakken og logg i oppsummeringen

### Revertering

```bash
# Reverter siste commit
git revert HEAD --no-edit
```


## Steg 4 — Push branchen

```bash
git push origin "$BRANCH"
```

Vent til CI-bygget er ferdig og grønt før du lager PR:

```bash
gh run watch $(gh run list --branch "$BRANCH" \
  --json databaseId --jq '.[0].databaseId') --exit-status
```

Hvis CI feiler: analyser feilen, reverter den aktuelle mergen, og logg i oppsummeringen.
**Ikke lag PR mot main med rødt bygg.**


## Steg 5 — Lag en oppsummerings-PR

```bash
gh pr create \
  --title "Ukentlige oppdateringer uke ${WEEK}" \
  --body "$(cat <<'EOF'
## Ukentlige avhengighetsoppdateringer

Denne PRen samler alle Dependabot-oppdateringer for uken.

### Inkluderte oppdateringer
<!-- Liste over merget PRer med pakkenavn og versjoner -->

### Skippet oppdateringer
<!-- PRer som ikke ble inkludert, med begrunnelse -->

### Kodeendringer utover versjons-bump
<!-- Eventuelle migrasjonstilpasninger som ble gjort (fjernede deprecations, API-endringer, etc.) -->

### Verifisering
- [ ] Alle tester passerer (`./gradlew test`)
- [ ] CI-bygg er grønt
- [ ] Breaking changes er håndtert

Merge til `main` etter godkjenning.
EOF
)" \
  --base main \
  --head "$BRANCH"
```


## Steg 6 — Instruksjon til mennesket

```
✅ Branch klar: oppdateringer/uke-<uke-nr>-<år>

Neste steg:
1. Sjekk at CI-bygget på branchen er grønt (Actions-tab)
2. Gjennomgå PR-en og eventuelle kodeendringer
3. Godkjenn og merge PR-en mot main

Merget i denne runden:
<liste over pakker som ble oppdatert>

Skippet (krever manuell gjennomgang):
<liste over pakker som ikke ble inkludert, med begrunnelse>
```


## Feilscenarioer og håndtering

| Scenario | Handling |
|----------|----------|
| Merge-konflikt i `build.gradle.kts` | Behold høyeste versjon, kjør `./gradlew dependencies`, commit |
| Merge-konflikt i `libs.versions.toml` | Behold høyeste versjon, kjør `./gradlew test` |
| Testfeil etter minor Spring Boot-bump | Sjekk release notes for deprecation-advarsler, tilpass kodebasen |
| Testfeil etter major-bump | Hent migreringsguide (se tabell over), tilpass kodebasen — reverter og logg hvis komplekst |
| Gradle plugin-inkompatibilitet | Sjekk plugin-changelog, reverter og logg |
| Dependabot-branch er utdatert | `gh pr comment <nr> --body "@dependabot rebase"` og vent |
| Major Kotlin-bump med type-feil | Les whatsnew-siden, tilpass typesignaturer — reverter hvis for komplekst |
