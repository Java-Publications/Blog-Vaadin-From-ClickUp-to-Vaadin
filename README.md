# Publikationsverwaltung — From ClickUp to Vaadin

A small, honest publication-management application that replaces a ClickUp-based
editorial workflow for blog posts and articles. It is the companion codebase to
the blog series **“From ClickUp to Vaadin”**: it shows, end to end, how a *flat*
ClickUp task list is reconstructed into a clean domain model, imported
idempotently, and driven through a Vaadin Flow UI — built on top of a hardened
jSentinel-secured Vaadin template.

> The scope is deliberately reduced to what the articles demonstrate. Billing,
> legal entities, invoicing/forecast logic and PDF composition are intentionally
> **out of scope** — the point is to follow the path *from the conflated ClickUp
> structure to a clean target structure* without commercial detours. The model is
> a genuine sub-slice of the full one and extends without breakage.

---

## The core idea

ClickUp knows exactly **one** level — the *task* — and presses into it what the
domain actually unfolds across **four content levels** and **three orthogonal
status dimensions**. The whole application exists to *reconstruct* that structure,
not merely transfer it.

**Four content levels** (the aggregate `Issue → Part → LanguageVersion → Publication`):

| Level | German (UI) | What it is |
|---|---|---|
| **Issue** | Thema | The topic bracket, e.g. *“Blog – Navigation – Coupled navigation”*. Carries topic/technology **tags** and an ordered list of parts. In ClickUp it only lived implicitly in the task-name prefix. |
| **Part** | Teil | A language-neutral content unit, identified by its issue + position. Carries exactly **one editorial state**. A topic may split into several parts to ship as a series. |
| **LanguageVersion** | Sprachfassung | The actual manuscript **per language** (DE/EN). The planned character count lives here because it differs per language. At most one version per language and part. |
| **Publication** | Veröffentlichung | Binds a language version to a **publication place**, with date, link and two of the three status dimensions. |

**Three orthogonal status dimensions** — the conflated ClickUp status field is
broken back into three independent progressions, each with a fixed order but free
transitions:

| Dimension | Carrier | Regular values | Terminal values |
|---|---|---|---|
| **Editorial work** | `Part` | `BACKLOG`, `IN_PLANNING`, `IN_PROGRESS`, `REVIEW` | `DONE`, `SKIPPED`, `CANCELLED` |
| **Production** | `Publication` | `PLANNED`, `PREPARED`, `PUBLISHED` | `UPDATE_NEEDED` (regression) |
| **Acquisition** | `Publication` | `REQUESTED`, `OFFERED`, `ACCEPTED`, `REVIEW` | `DONE`, `SKIPPED`, `CANCELLED` |

Orthogonality is the whole point: an acquisition can already be `ACCEPTED` while
production is still `PREPARED` and the underlying part is only `IN_PROGRESS`. In
the single ClickUp field one progress had to overwrite the others; here they run
side by side. (This is also why ClickUp’s *“paid”* and *“on hold”* values
disappear: “paid” is a property of the invoice, not the content; “on hold” is a
rest across all dimensions, not a phase within one.)

---

## Domain model

```
DataRoot                       ← Eclipse-Store object root
├── issues: List<Issue>
│     Issue (Thema)            id, title, description, tags, origin*
│     └── parts: List<Part>    *origin = external id for idempotent import
│           Part (Teil)        position, editorialWork: StatusHistory<EditorialState>
│           └── languageVersions: List<LanguageVersion>   (≤ 1 per Language)
│                 LanguageVersion (Sprachfassung)   manuscript, plannedCharacters
│                 └── publications: List<Publication>
│                       Publication (Veröffentlichung)
│                         place, date, link, client,
│                         production:  StatusHistory<ProductionStatus>
│                         acquisition: StatusHistory<AcquisitionStatus>
├── publicationPlaces: List<PublicationPlace>   name + supported languages
└── clients: List<Client>                       curated “Auftraggeber” master data
```

**Key invariants (enforced in the model, not the UI):**

- **Append-only history.** Every status change of every dimension is an immutable
  event recorded through the encapsulated `StatusHistory<T>`. Its order rests on a
  strictly ascending sequence number — never on list position or timestamp — and
  the *current* state is always read from the last event, so it can never diverge
  from the history. Three independent chains hang off the same content and *are*
  the proof of orthogonality.
- **Language rule.** A `Publication` may only target a `PublicationPlace` whose
  supported languages include the version’s language. `LanguageVersion.canBePublishedAt(place)`
  checks it without mutating; `Publication`’s constructor and `setPlace` enforce it
  hard (`IllegalArgumentException` otherwise).
- **At most one version per language** per part; positions are reassigned `1..n`
  on `reorderParts`, which touches content order only, never history.
- **`origin`** carries the external ClickUp task id, which makes import idempotent.

The domain lives under `com.svenruppert.publications.model` and is pure Java with
no framework dependencies — persistence and UI sit strictly around it.

---

## ClickUp import (ETL)

`com.svenruppert.publications.importetl.ClickUpImportService` implements a genuine
**Extract → Transform → Load** so the (rate-limited, paginated) network fetch is
separated from the repeatable transform:

1. **Extract** — `extract(token, listId)` walks the ClickUp task endpoint
   (`api.clickup.com/api/v2/list/{id}/task?include_closed=true&page=N`), which is
   capped at 100 tasks per page, concatenating every raw task node into one
   `{"tasks":[…]}` document. Every original field is preserved.
2. **Cache** — `RawImportStore` persists the raw JSON plus a last-extraction
   timestamp under the storage dir, so transform/load survives a restart and can
   re-run offline against the local copy.
3. **Transform + Load** — `transformAndLoad(rawJson, repo, listener)` rebuilds the
   levels from the flat task, distributes the conflated status onto the editorial
   dimension via `mapStatus(...)`, and loads **idempotently**: an issue already
   known by its `origin` is skipped, so a repeat run creates no duplicates. A
   `ProgressListener` drives a determinate progress bar; an `ImportReport` returns
   `created / skipped` counts and the status-mapping distribution.

> Only editorial states are mapped today; acquisition/production statuses on a
> ClickUp task currently fall to `BACKLOG` by design (see the docs for the known
> fidelity gap). Import is a throwaway tool for production use, a workhorse in dev.

---

## Screens & routes

All views use `MainLayout` (role-gated drawer, locale/theme switch). Routes keep
their German path segments; each view exposes a `NAV` constant.

| Route | View | Purpose | Guard |
|---|---|---|---|
| `topics` | `TopicsView` | Topic workspace — master issue grid + detail, tags, parts, view-local filters (Any/All tags, multi-status) | login |
| `part/:id` | `LanguageVersionView` | Language-version (Sprachfassung) editor | login |
| `publication` | `PublicationView` | Single publication — status dimensions, place, second-use (Zweitverwertung) | login |
| `publications` | `PublicationListView` | Publications table — resizable columns + complex multi-criteria search | login |
| `editorial` | `EditorialBoardView` | Editorial board — Kanban by editorial state + table view, drag & drop, AND/OR tag filter | login |
| `history` | `HistoryView` | Append-only status history | login |
| `places` | `PublicationPlacesView` | Publication places master data | `masterdata:edit` |
| `clients` | `ClientsView` | Clients (Auftraggeber) master data | `masterdata:edit` |
| `import` | `ImportView` | ClickUp import console — progress bar, live log, result panel | `publications:import` |

Reusable UI building blocks live in `com.svenruppert.flow.views.ui`
(`PageHeader`, `FilterBar`, `BackButton`, `EmptyState`, `MetricTile`, `BrandMark`,
`LocaleSwitcher`, `ThemeSwitcher`, …); shared publication helpers in
`PublicationUi`.

---

## Security & access control

Authentication and authorization are provided by **jSentinel** (inherited from the
underlying template). Two roles map onto a small permission catalog
(`com.svenruppert.flow.security.permissions.AppPermission`):

| Permission | Meaning |
|---|---|
| `publications:read` | View publication data |
| `publications:edit` | Create/modify topics, parts, versions, publications |
| `masterdata:edit` | Maintain publication places and clients |
| `publications:import` | Run the ClickUp import |

Roles are `ADMIN` and `USER` (`AuthorizationRole`); views are gated with
`@RequiresPermission(...)` and the drawer is filtered by grant. On first start the
app prints a **bootstrap token** to stdout and writes it to
`./data/jsentinel/bootstrap.token`; open `/login`, you are redirected to `/setup`
to create the first admin. The layered bootstrap SPI, audit log, session admin and
password preflight come from the underlying template.

---

## Persistence & storage

- **Eclipse-Store** object graph rooted at `DataRoot`, behind the
  `PublicationsPersistence` port. Two implementations ship:
  `EclipseStorePublicationsPersistence` (production) and
  `InMemoryPublicationsPersistence` (tests). `PublicationsProvider` resolves the
  active one; `PublicationsRepository` is the application-facing API
  (`issues()`, `createIssue`, `findIssueByOrigin`, `allPublications`, `placesFor`,
  `clients()`, `persist()`, …).
- **Schema evolution:** Eclipse-Store does not run field initializers when loading
  an older graph, so fields added later (e.g. `DataRoot.clients`) load as `null`
  and every accessor null-guards. Keep that in mind when adding fields.
- **Storage location:** `AppStoragePaths` is the single source of truth. Production
  defaults to `./data`; override with `-Dapp.storage.dir=/some/path`. Tests fork
  with `-Dapp.storage.dir=target/test-data` so they never touch the repo-rooted
  `./data/` tree.

---

## Internationalization

UI strings live in `src/main/resources/vaadin-i18n/translations*.properties`
(`translations.properties` = English ground truth, `translations_de.properties` =
German). Views implement `I18nSupport` and call `tr(key, "English fallback")`. A
custom `AppI18NProvider` defeats the JVM-default-locale `ResourceBundle` fallback
bug (Vaadin 25 ignores `META-INF/services` for `I18NProvider`). Key parity between
the two bundles is enforced by `I18nBundleLoadingTest`.

> **Language rule for the codebase:** source code — identifiers, comments, log and
> exception messages — is American English. Only user-facing i18n resources carry
> the target languages.

---

## Tech stack

| Component | Version |
|---|---|
| Java (`maven.compiler.release`) | **26** |
| Vaadin Flow (`vaadin-core`, Hilla disabled) | **25.1.1** |
| Jetty | **12.1.8** |
| Packaging | **WAR** (standalone fat-jar via `_shadejar`) |
| Parent POM (`com.svenruppert:dependencies`) | **06.02.02** |
| jSentinel (security) | **00.74.00** |
| nano-vaadin-jetty (fat-jar) | **04.00.00** |
| Persistence | Eclipse-Store |

`pom.xml` is the authoritative version reference; `CLAUDE.md` / `AGENTS.md` mirror
it. Cross-check with `./mvnw versions:display-dependency-updates`.

---

## Getting started

```bash
./mvnw                       # dev server, default goal = jetty:run on http://localhost:8080
```

First start prints a bootstrap token; open `http://localhost:8080/login` → you are
sent to `/setup` to create the first admin. Then sign in and, from the drawer, run
the **Import** (needs `publications:import`) to pull a ClickUp list, or start
creating topics in the **Topics** workspace.

### Build commands

```bash
./mvnw                                              # dev server (jetty:run)
./mvnw compile                                      # compile only
./mvnw test                                         # unit + browserless tests
./mvnw test -Dtest=ClassName#methodName             # a single test
./mvnw package -Pproduction                         # optimized production WAR -> target/ROOT.war
./mvnw -P_shadejar -DskipTests package              # standalone Jetty fat-jar (application.jar)
./mvnw -P_mutation-gate \
       org.pitest:pitest-maven:mutationCoverage \
       verify                                       # PIT mutation coverage + per-package gate
./mvnw versions:display-dependency-updates          # dependency audit
```

**Profiles:** `production` (optimized frontend bundle), `_java` (ASM 9.8 for Java 26),
`_shadejar` (fat-jar `application.jar`), `_mutation-gate` (PIT + `tools/pit-gate.sh`
per-package floors).

---

## Testing & quality gates

- **Browserless tests** (`com.vaadin.browserless.BrowserlessTest`) drive views
  without a browser; test classes live under `junit.com.svenruppert.*` and seed
  state with `TestSupport.seedAdminAndResetBootstrap()`. 307 tests at the current
  release.
- **No mocks.** Tests use real implementations (e.g. `InMemoryPublicationsPersistence`),
  not mock frameworks.
- **SpotBugs** runs on `verify` (bound via the parent); intentional exposures carry
  `@SuppressFBWarnings(value, justification)`. Standing rule: run SpotBugs and clear
  all findings after every code change, not just at release.
- **Mutation gate** — PIT floors per package in `tools/pit-baselines.txt`; the build
  fails on regression. Add a line per new package.

---

## Project layout

```
src/main/java/com/svenruppert/
├── publications/                      ← framework-free domain
│   ├── model/                         Issue, Part, LanguageVersion, Publication,
│   │                                  PublicationPlace, Client, StatusHistory,
│   │                                  Editorial/Production/AcquisitionStatus, DataRoot
│   ├── persistence/                   PublicationsPersistence port + Eclipse-Store /
│   │                                  in-memory impls, Provider, Repository
│   └── importetl/                     ClickUpImportService, RawImportStore, ImportReport
└── flow/
    ├── AppShell / AppServlet          @Push, theme, i18n provider wiring
    ├── i18n/                          AppI18NProvider
    ├── security/                      jSentinel: bootstrap, roles, permissions, services
    └── views/
        ├── publications/              the nine publication views + PublicationUi + dialogs
        ├── ui/                        design-system building blocks
        └── MainLayout, Dashboard, Login, Setup, Admin, Audit, Sessions, …
```

---

## Documentation

Deeper concept and process docs live in [`docs/`](docs/):

- [`Fachkonzept-Publikationsverwaltung-Blogpost.md`](docs/Fachkonzept-Publikationsverwaltung-Blogpost.md) — the domain concept (the *what* and *why*).
- [`Datenmodell-Blogpost-vereinfacht-Typskizzen.md`](docs/Datenmodell-Blogpost-vereinfacht-Typskizzen.md) — the type sketches.
- [`Prozesskatalog-Publikationsverwaltung-Blogpost.md`](docs/Prozesskatalog-Publikationsverwaltung-Blogpost.md) — the process catalogue.
- [`View-Briefs-Publikationsverwaltung-Blogpost.md`](docs/View-Briefs-Publikationsverwaltung-Blogpost.md) — per-view briefs.
- [`DESIGN_SYSTEM.md`](docs/DESIGN_SYSTEM.md) — theme tokens & reusable components.
- [`prompt-log.md`](docs/prompt-log.md) — the full prompt history mapped to the commits that implemented each one.

---

## Development process

Work follows the `implementation-cycle` backbone (five stages, risk-first order,
entry/exit review gates, no mocks). Concretely for this repo:

- **Tracker:** ClickUp — plan + per-issue in list `publikationsverwaltung`; status
  `Open → in progress → Closed`, with a `## Completion log` appended per task.
- **Versioning:** single-module WAR on a `-SNAPSHOT` working line, finalize-strip on
  release, tag scheme `v<X.Y.Z>`. **Releases are cut only on explicit request** —
  bugs and features accumulate on the SNAPSHOT line until then. See
  [`RELEASE_NOTES.md`](RELEASE_NOTES.md) (current: **v00.40.00**).
- **Git:** origin is a self-hosted ForgeJo; GitHub is a server-side push mirror.
  Commit messages carry no AI-attribution footer.

---

## License

European Union Public Licence 1.2 (EUPL-1.2) — see the per-file header that every
source carries, and `pom.xml` for the license metadata.
