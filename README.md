This is a Kotlin Multiplatform project targeting Android, iOS, Desktop (JVM).

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- Desktop app:
  - Hot reload: `./gradlew :desktopApp:hotRun --auto`
  - Standard run: `./gradlew :desktopApp:run`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- Desktop tests: `./gradlew :shared:jvmTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

---




# Kumbaya

A universal backend plug built with Kotlin Multiplatform. Any data source — JSON URL, SQL database,
or open-data catalog — is queryable through the same interfaces, with a RAG pipeline and MCP server
layered on top. Targets Android, iOS, and Desktop (JVM).

On top of that universal layer sit **personas** — self-contained feature bundles modeled on specific
user needs, each with its own directory under `rag/personas/<name>/` (data + deterministic logic),
`mcp/personas/<name>/` (MCP tool registration), and optionally `ui/personas/<name>/` (an in-app
screen). Two exist today:

- **Fiber Construction Manager** — pre-construction intelligence (GIS, permitting, environmental,
  pole attachment) for an already-decided fiber build, anywhere in the US.
- **Fiber Entrepreneur** — market opportunity analysis for a prospective fiber build: real FCC
  broadband data + optional Census demographics, a deterministic 0–100 Fiber Opportunity Score
  (computed in Kotlin, never guessed by the LLM), and an AI-narrated report. Available both as an
  MCP tool and as a dashboard screen in the app.

Adding a new persona means adding a new directory and one registration call — never editing an
existing persona's files. See "Project structure" below for exactly where each layer lives; that
structure *is* the architecture reference for this repo.

---

## What it does

- **Catalog mode** — search Socrata, ArcGIS Hub, and data.gov simultaneously by topic. Tap any
  result to load its full table, then ask questions about it grounded in a RAG pipeline over that
  table's rows.
- **Fiber Entrepreneur dashboard** — enter a US county's FIPS codes and get a live Fiber Opportunity
  Score, broadband gap analysis, optional demographics, and an AI-narrated report.
- **MCP server** — exposes all data, RAG, and persona capabilities as eight tools consumable by any
  MCP client (Claude Desktop, agents, etc.): four general-purpose (`fetch_json`, `query_sql`, `ask`,
  `search_datasets`) and four persona-scoped (`find_dataset`, `discover_and_ask`,
  `fiber_pre_construction`, `market_opportunity_report`).

---

## Prerequisites

### 1. OpenAI API key (required — embeddings and generation, every platform)

Every platform (Desktop, Android, iOS) and the MCP server use OpenAI for embeddings and generation.
Get a key at https://platform.openai.com.

```bash
echo 'export OPENAI_API_KEY="sk-your-key-here"' >> ~/.zshrc && source ~/.zshrc
```

### 2. Census API key (optional — adds demographics to the Fiber Entrepreneur report)

Without this, `market_opportunity_report` and the Fiber Entrepreneur dashboard still work — they
just report broadband-only figures with demographics explicitly marked "not configured," rather
than failing. Get a free key at https://api.census.gov/data/key_signup.html (instant, just an
email + org name). Currently wired for Desktop and the MCP server only.

```bash
echo 'export CENSUS_API_KEY="your-key-here"' >> ~/.zshrc && source ~/.zshrc
```

### 3. Ollama (optional — local/offline LLM swap)

Not required to run anything by default; the app and MCP server are wired to OpenAI. Ollama remains
a one-line constructor swap (`OllamaClient` implements the same `LlmClient` interface) for a fully
local, free demo, and a handful of `rag` tests exercise it directly (see "Running tests" below).

```bash
brew install ollama
ollama pull nomic-embed-text   # embedding model
ollama pull llama3.2           # generation model
ollama serve                   # keep running while using Ollama-backed tests
```

---

## Secret setup per platform

| Secret | Desktop | MCP server | Android | iOS |
|---|---|---|---|---|
| `OPENAI_API_KEY` | `~/.zshrc` | `~/.zshrc` | `local.properties` | `Info.plist` |
| `CENSUS_API_KEY` | `~/.zshrc` | `~/.zshrc` | not wired yet | not wired yet |

**Android (`local.properties`)** — add this line to `local.properties` in the project root (gitignored):

```
OPENAI_API_KEY=sk-your-key-here
```

Sync Gradle after editing (File → Sync Project with Gradle Files).

**iOS (`iosApp/iosApp/Info.plist`)** — replace the placeholder value for `OPENAI_API_KEY`:

```xml
<key>OPENAI_API_KEY</key>
<string>sk-your-key-here</string>
```

Do not commit your actual key — restore the placeholder before pushing.

---

## Running the apps

| Platform | Command |
|---|---|
| Desktop | `./gradlew :desktopApp:run` |
| Desktop (hot reload) | `./gradlew :desktopApp:hotRun --auto` |
| Android | `./gradlew :androidApp:assembleDebug` or run from Android Studio |
| iOS | Open `/iosApp` in Xcode and run |

## Running the MCP server

```bash
./gradlew :mcpServer:installDist
```

Then point your MCP client (e.g. Claude Desktop) at the installed launcher. The server reads
`OPENAI_API_KEY` from the environment at startup and fails immediately with a clear message if it's
not set. `CENSUS_API_KEY` is read the same way but is optional.

## Running tests

```bash
./gradlew :rag:jvmTest          # RAG + persona tests (some require Ollama running, see below)
./gradlew :mcpServer:test       # MCP end-to-end stdio tests
./gradlew :shared:jvmTest       # Shared module tests
./gradlew :core:jvmTest         # Core module tests
```

Ollama must be running for: `RagPipelineIngestTest`, `RagPipelineAnswerTest`,
`RegistrySourceResolverTest`, `OllamaClientLiveTest`. Everything else in `rag:jvmTest`
(`InMemoryVectorIndexTest`, `CatalogSourceResolverLiveTest`) and all of `core:jvmTest`
(`SqlDataSourceTest`) run without it.

## Project structure

```
core/        — Data sources, HTTP client, domain types
rag/         — LLM clients, catalog resolvers, RAG pipeline
  personas/
    fiberConstructionManager/  — pre-construction intelligence data + logic
    fiberEntrepreneur/         — FCC/Census data, deterministic scoring, report pipeline
mcpServer/   — MCP tool exposure over stdio (JVM only); Main.kt is the composition root
  personas/
    fiberConstructionManager/  — find_dataset, discover_and_ask, fiber_pre_construction
    fiberEntrepreneur/         — market_opportunity_report
shared/      — Compose UI + ViewModels (Android, Desktop, iOS); App.kt is the composition root
  ui/personas/
    fiberEntrepreneur/         — the Fiber Entrepreneur dashboard screen
iosApp/      — iOS Xcode project entry point
androidApp/  — Android application entry point
desktopApp/  — Desktop application entry point
```