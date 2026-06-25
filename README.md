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

For a full breakdown of the architecture, swap points, and how to extend each layer, see
[ARCHITECTURE.md](./ARCHITECTURE.md).

  ---

## What it does

- **Catalog mode** — search Socrata, ArcGIS Hub, and data.gov simultaneously by topic. Tap any
  result to load its full table.
- **Ask the Web mode** — ask any question. The app fetches live web results via Tavily and grounds
  the LLM answer in them.
- **MCP server** — exposes all data and RAG capabilities as six tools consumable by any MCP client
  (Claude Desktop, agents, etc.).

  ---

## Prerequisites

### 1. Ollama (local LLM — required for MCP server and RAG tests)

  ```bash
  brew install ollama
  ollama pull nomic-embed-text   # embedding model
  ollama pull llama3.2           # generation model
  ollama serve                   # keep running while using MCP tools or RAG tests

  2. OpenAI API key (required for MCP server)

  The MCP server uses OpenAI for embeddings and generation. Get a key at
  https://platform.openai.com and add it to your shell:

  echo 'export OPENAI_API_KEY="sk-your-key-here"' >> ~/.zshrc && source ~/.zshrc

  3. Tavily API key (required for Ask the Web feature)

  Get a free key at https://tavily.com (1000 searches/month free) and add it to your shell:

  echo 'export TAVILY_API_KEY="tvly-your-key-here"' >> ~/.zshrc && source ~/.zshrc

 ---
  Secret setup per platform

  ┌────────────────┬────────────────────────────┬──────────────────┬──────────────────────────┐
  │     Secret     │          Desktop           │     Android      │           iOS            │
  ├────────────────┼────────────────────────────┼──────────────────┼──────────────────────────┤
  │ TAVILY_API_KEY │ ~/.zshrc                   │ local.properties │ iosApp/iosApp/Info.plist │
  ├────────────────┼────────────────────────────┼──────────────────┼──────────────────────────┤
  │ OPENAI_API_KEY │ ~/.zshrc (MCP server only) │ not required     │ not required             │
  └────────────────┴────────────────────────────┴──────────────────┴──────────────────────────┘

  Android (local.properties)

  Add these lines to local.properties in the project root (this file is gitignored):

  TAVILY_API_KEY=tvly-your-key-here
  OPENAI_API_KEY=sk-your-key-here
  
  Sync Gradle after editing (File → Sync Project with Gradle Files).

  iOS (iosApp/iosApp/Info.plist)

  Replace the placeholder value for TAVILY_API_KEY in Info.plist:

  <key>TAVILY_API_KEY</key>
  <string>tvly-your-key-here</string>

  Do not commit your actual key — restore the placeholder before pushing.

  ---
  Running the apps
  
  ┌──────────────────────┬────────────────────────────────────────────────────────────────┐
  │       Platform       │                            Command                             │
  ├──────────────────────┼────────────────────────────────────────────────────────────────┤
  │ Desktop              │ ./gradlew :desktopApp:run                                      │
  ├──────────────────────┼────────────────────────────────────────────────────────────────┤
  │ Desktop (hot reload) │ ./gradlew :desktopApp:hotRun --auto                            │
  ├──────────────────────┼────────────────────────────────────────────────────────────────┤
  │ Android              │ ./gradlew :androidApp:assembleDebug or run from Android Studio │
  ├──────────────────────┼────────────────────────────────────────────────────────────────┤
  │ iOS                  │ Open /iosApp in Xcode and run                                  │
  └──────────────────────┴────────────────────────────────────────────────────────────────┘

  Running the MCP server
  
  ./gradlew :mcpServer:installDist

  Then point your MCP client (e.g. Claude Desktop) at the installed launcher. The server reads
  OPENAI_API_KEY from the environment at startup and will fail immediately with a clear message
  if it is not set.

  ---
  Running tests

  ./gradlew :rag:jvmTest          # RAG pipeline tests (requires Ollama running)
  ./gradlew :mcpServer:test       # MCP end-to-end stdio tests
  ./gradlew :shared:jvmTest       # Shared module tests
  ./gradlew :core:jvmTest         # Core module tests

  Tests that do not require Ollama (InMemoryVectorIndexTest, SqlDataSourceTest) can be run
  at any time.

  ---
  Project structure

  mcpServer/   — MCP tool exposure over stdio (JVM only)
  shared/      — Compose UI + ViewModels (Android, Desktop, iOS)
  rag/         — LLM clients, catalog resolvers, RAG pipeline
  core/        — Data sources, HTTP client, domain types
  iosApp/      — iOS Xcode project entry point
  androidApp/  — Android application entry point
  desktopApp/  — Desktop application entry point