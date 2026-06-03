# boe-pipeline

> A data ingestion pipeline for the **Spanish Official State Gazette (BOE)** built on top of [Spool](https://github.com/spool-framework), a reactive data pipeline framework for Java.

---

## What does this project do?

This example demonstrates how to use Spool to build a complete pipeline that:

1. **Polls daily** the BOE open data API (`boe.es/datosabiertos`)
2. **Normalizes** the gazette's hierarchical JSON structure (sections → departments → epigraphs → items) into a flat list of individual events
3. **Maps** each item to a typed domain event (`BoeItem`)
4. **Persists** events to a local data lake via the ingester module
5. **Cleans up** expired events automatically through the janitor module

---

## Pipeline architecture

The entire pipeline is declared in a single YAML file (`pipeline-boe.yaml`):

```yaml
modules:
  - crawler:        # Fetches and normalizes the source
      type: POLL
      source:
        type: HTTP  # Custom plugin: BOEHTTPPollSource

  - ingester:       # Persists events to the data lake
      type: REACTIVE

  - janitor:        # Removes events that exceed the configured TTL
```

```
┌─────────────────────────────────────────────────────────┐
│                    Spool Runtime                        │
│                                                         │
│  ┌──────────┐    ┌───────────┐    ┌───────────────┐    │
│  │ Crawler  │───▶│ Event Bus │───▶│   Ingester    │    │
│  │  (POLL)  │    │(IN_MEMORY)│    │  (REACTIVE)   │    │
│  └──────────┘    └───────────┘    └───────┬───────┘    │
│       │                                   │            │
│  BOE Open API                      Data Lake (FS)      │
│  (HTTP)                                                 │
│                          ┌────────────┐               │
│                          │   Janitor  │               │
│                          │   (TTL)    │               │
│                          └────────────┘               │
└─────────────────────────────────────────────────────────┘
```

---

## Custom plugins

Spool uses a Java SPI-based plugin system. This example implements two plugins:

### `BOEHTTPPollSource` / `BOEHTTPPollSourceProvider`

A POLL source that downloads the current day's gazette summary from the BOE API. Today's date is automatically appended to the base URL in `YYYYMMDD` format.

```java
@SpoolPlugin(PollSourceProvider.class)
public class BOEHTTPPollSourceProvider implements PollSourceProvider {
    @Override public String name() { return "BOE_HTTP"; }
    // ...
}
```

### `BOEJSONArrayNormalizerProvider` + `NormalizeSummary`

A normalizer that runs the raw BOE payload through a transformation pipeline:

```
byte[] raw
  → DeserializeStep     (JSON bytes → JsonNode)
  → NormalizeSummary    (flattens sections/departments/epigraphs → item list)
  → LocateStep          (navigates to the rootPath declared in YAML)
  → SplitEnrichStep     (splits the array into individual enriched events)
  → SerializeStep       (JsonNode → byte[])
```

---

## Domain event model

Each BOE item is represented as a Java record implementing Spool's `Event` interface:

```java
public record BoeItem(
    String identifier,
    String title,
    String correlationId,   // Daily summary ID
    String publishDate,     // Publication date (YYYYMMDD)
    String sectionCode,
    String sectionName,
    String departmentCode,
    String departmentName,
    String epigraphName,
    String urlHtml,
    String urlXml,
    String urlPdf
) implements Event { ... }
```

---

## Project structure

```
boe-pipeline/
├── src/main/java/
│   ├── events/
│   │   └── BoeItem.java                           # Domain event model
│   └── software/examples/spool/boe/
│       ├── Application.java                        # Runtime configuration
│       ├── Main.java                               # Entry point
│       └── plugins/
│           ├── BOEHTTPPollSource.java              # HTTP client for the BOE API
│           ├── BOEHTTPPollSourceProvider.java      # SPI plugin: poll source
│           ├── BOEJSONArrayNormalizerProvider.java # SPI plugin: normalizer
│           └── NormalizeSummary.java               # Summary flattening step
├── src/main/resources/
│   └── pipeline-boe.yaml                          # Pipeline declaration
└── pom.xml
```

---

## Requirements

| Tool  | Minimum version |
|-------|----------------|
| Java  | 21             |
| Maven | 3.8+           |
| Spool | 1.0.0-SNAPSHOT |

---

## Running the pipeline

### 1. Build

```bash
mvn package
```

### 2. Run

```bash
java -jar target/boe-pipeline.jar
```

The pipeline will start, fetch today's BOE summary, and store each item as an event in the configured data lake.

### 3. Configure paths

Edit `src/main/resources/pipeline-boe.yaml` to change the inbox and data lake locations:

```yaml
infrastructure:
  inbox:
    type: FILE_SYSTEM
    configuration:
      path: "/your/inbox/path"

  dataLake:
    type: FILE_SYSTEM
    configuration:
      path: "/your/datalake/path"
```

---

## Observability

The application exports traces, metrics, and logs via **OpenTelemetry**. By default it targets a local collector:

| Signal  | Endpoint                               |
|---------|----------------------------------------|
| Logs    | `http://localhost:3100/otlp/v1/logs`   |
| Metrics | `http://localhost:4320/v1/metrics`     |
| Traces  | `http://localhost:4318/v1/traces`      |

Compatible with any OTLP-capable backend (Grafana, Jaeger, Prometheus, etc.).

---

## Context

This project is part of the **Spool Framework** Labs collection. Spool is a Java framework for building data ingestion pipelines through YAML configuration and an extensible SPI plugin system, developed as part of a Final Degree Project.

More examples available at the [`spool-framework`](https://github.com/Spool-FRAMEWORK-Labs) organization.

---

## License

MIT
