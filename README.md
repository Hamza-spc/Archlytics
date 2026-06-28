# Archlytics

**AI Software Architect for Java codebases**

Archlytics reads a Java repository, builds a module dependency graph, detects architectural violations with deterministic rules, generates Mermaid diagrams, and uses an LLM to interpret system design risks and scaling bottlenecks.

> Static analysis first, AI second — the graph is built from imports, not guessed from source code.

---

## Example output

Analysis of a 3-module Maven project ([maroctax](https://github.com/Hamza-spc/maroctax)):

```text
Java files: 28
Modules: 3
Violations: 6
Longest chain: maroctax-api → maroctax-spring-boot-starter → maroctax-core

[HIGH]   Shared kernel bottleneck — maroctax-core depended on by 2 modules
[MEDIUM] Layer bypass — api depends on core while also using starter
[MEDIUM] Serial dependency chain — 3-hop synchronous path
```

### Layer view (auto-generated)

```mermaid
graph TB
  subgraph L0 [Presentation Layer]
    maroctax_api[maroctax-api]
  end
  subgraph L1 [Integration Layer]
    maroctax_spring_boot_starter[maroctax-spring-boot-starter]
  end
  subgraph L2 [Domain Layer]
    maroctax_core[maroctax-core]
  end
  maroctax_api --> maroctax_core
  maroctax_api --> maroctax_spring_boot_starter
  maroctax_spring_boot_starter --> maroctax_core
```

See [docs/sample-report.md](docs/sample-report.md) for a full example report, or open [docs/sample-report.html](docs/sample-report.html) for the styled HTML version (health score **55/100** on maroctax).

---

## Pipeline

```mermaid
flowchart LR
  A[Ingest] --> B[Graph]
  B --> C[Rules]
  C --> D[Diagrams]
  D --> E[AI]
  E --> F[Report]

  A --- A1["Scan .java files"]
  B --- B1["Parse imports"]
  C --- C1["Circular deps, coupling, bottlenecks"]
  D --- D1["Mermaid diagrams"]
  E --- E1["Groq / Gemini"]
  F --- F1["archlytics-report.md"]
```

| Stage | What it does |
|-------|----------------|
| **Ingest** | Walks the repo, skips `target/`, `node_modules`, etc. |
| **Graph** | Regex-parses `import` statements, resolves internal types, groups by Maven module |
| **Rules** | Circular dependencies, high coupling, shared-kernel bottlenecks, layer bypasses |
| **Diagrams** | Module graph, layer view, critical path — deterministic, no AI |
| **AI** | Architecture summary, scaling risks, recommendations (Groq or Gemini) |
| **Report** | Markdown with violations, metrics, and embedded Mermaid |

---

## Quick start

**Requirements:** Java 21+, Maven 3.9+

```bash
git clone https://github.com/Hamza-spc/Archlytics.git
cd Archlytics

# Configure AI (optional — diagrams + rules work without it)
cp .env.example .env
# Edit .env and add GROQ_API_KEY from https://console.groq.com

mvn -q package
java -jar target/archlytics-0.1.0-SNAPSHOT.jar /path/to/your/java/repo
```

Output is written to `archlytics-report.md` in the current directory. Every report includes an **architecture health score** (0–100) computed deterministically from violations and graph shape.

### CLI options

```bash
# Custom report path
java -jar target/archlytics-0.1.0-SNAPSHOT.jar ./my-repo -o report.md

# HTML report (styled, Mermaid diagrams, print-to-PDF button)
java -jar target/archlytics-0.1.0-SNAPSHOT.jar ./my-repo --format html

# Both markdown and HTML
java -jar target/archlytics-0.1.0-SNAPSHOT.jar ./my-repo --format both

# Skip AI — graph, rules, and diagrams only (no API key needed)
java -jar target/archlytics-0.1.0-SNAPSHOT.jar ./my-repo --skip-ai

# Save snapshot and compare architecture drift over time
java -jar target/archlytics-0.1.0-SNAPSHOT.jar ./my-repo --save-snapshot
java -jar target/archlytics-0.1.0-SNAPSHOT.jar ./my-repo --compare latest
```

---

## Configuration

Create `archlytics.yaml` in the repo you analyze (or pass `--config path/to/archlytics.yaml`):

```yaml
rules:
  highCoupling:
    fileImportThreshold: 6
    moduleFanOutThreshold: 3
    moduleFanInThreshold: 3
  systemDesign:
    serialChainThreshold: 3
    hubFanInThreshold: 2
    entryFanOutThreshold: 2

ignore:
  modules:
    - legacy-module
  directories:
    - generated-sources
  pathPatterns:
    - "**/generated/**"

ai:
  enabled: true
  provider: groq   # optional: groq or gemini
```

See [archlytics.yaml.example](archlytics.yaml.example) for all options.

### Environment variables (AI keys)

Create a `.env` file in the Archlytics project root (never commit it):

```env
AI_PROVIDER=groq
GROQ_API_KEY=gsk_your_key_here

# Optional
# GROQ_MODEL=llama-3.3-70b-versatile
# GEMINI_API_KEY=...        # alternative provider
# AI_PROVIDER=gemini
```

| Variable | Description |
|----------|-------------|
| `AI_PROVIDER` | `groq` or `gemini` (auto-detected from keys if omitted) |
| `GROQ_API_KEY` | Free key from [console.groq.com](https://console.groq.com) |
| `GROQ_MODEL` | Default: `llama-3.3-70b-versatile` |
| `GEMINI_API_KEY` | Free key from [Google AI Studio](https://aistudio.google.com/apikey) |

---

## Architecture health score

Deterministic scoring (not AI):

| Penalty | Amount |
|---------|--------|
| HIGH violation | −15 |
| MEDIUM violation | −8 |
| LOW violation | −3 |
| Circular dependency | −5 |
| Deep dependency chain | −5 |

| Score | Label |
|-------|-------|
| 80–100 | Healthy |
| 60–79 | Needs attention |
| 0–59 | Critical |

## Architecture drift

Save snapshots in the analyzed repository and compare runs over time:

```bash
# First run — save baseline
java -jar target/archlytics-0.1.0-SNAPSHOT.jar ./my-repo --save-snapshot --skip-ai

# Later run — compare against latest snapshot
java -jar target/archlytics-0.1.0-SNAPSHOT.jar ./my-repo --compare latest --skip-ai
```

Snapshots are stored in `<repo>/.archlytics/snapshots/` as JSON. Reports include score drift, new violations, and resolved violations.

## Pull request analysis

Compare a PR branch against its base ref and flag architecture regressions:

```bash
java -jar target/archlytics-0.1.0-SNAPSHOT.jar ./my-repo --base origin/main --head HEAD --skip-ai
```

Reports include changed files/modules, new module dependencies, and violations introduced compared to base.

## GitHub Action

Run Archlytics on every pull request and post a summary comment:

```yaml
# .github/workflows/archlytics.yml
name: Archlytics
on:
  pull_request:
    branches: [main]

jobs:
  architecture:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      pull-requests: write
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - uses: Hamza-spc/Archlytics/action@main
        with:
          fail-on: high        # fail check if PR introduces HIGH violations
          skip-ai: true        # no API key needed
          # groq-api-key: ${{ secrets.GROQ_API_KEY }}  # optional AI
```

The action builds Archlytics, compares the PR against its base branch, uploads a report artifact, and updates a PR comment (idempotent — one comment per PR).

**Live example:** [maroctax](https://github.com/Hamza-spc/maroctax) runs this action on every Java PR — see [`.github/workflows/archlytics.yml`](https://github.com/Hamza-spc/maroctax/blob/main/.github/workflows/archlytics.yml).

### CI exit codes

Use `--fail-on` locally or in custom CI:

```bash
java -jar target/archlytics-0.1.0-SNAPSHOT.jar ./my-repo --fail-on high --skip-ai
java -jar target/archlytics-0.1.0-SNAPSHOT.jar ./my-repo --base origin/main --fail-on medium --pr-comment comment.md
```

| `--fail-on` | Fails when |
|-------------|------------|
| `none` | Never (report only) |
| `high` | Any HIGH violation |
| `medium` | MEDIUM or HIGH |
| `low` | Any violation |

With `--base`, only **introduced** PR violations are checked. Without `--base`, all current violations are checked.

## What it detects

### Deterministic rules (no AI)

- **Circular dependencies** — module cycles like `A → B → C → A`
- **High coupling** — modules or files with excessive fan-in / fan-out
- **Shared kernel bottleneck** — one module depended on by many others
- **Layer bypass** — entry module skipping an intermediate layer
- **Serial dependency chains** — deep synchronous call paths
- **Entry fan-out** — API layer depending on multiple backends

### AI interpretation

- Architecture type (e.g. layered modular monolith)
- System design issues with impact levels
- Scaling scenarios (10x traffic, concurrency spikes)
- Actionable refactor recommendations

---

## Project structure

```
src/main/java/com/archlytics/
├── cli/           # Picocli entry point
├── ingest/        # File scanner
├── graph/         # Import parser, dependency graph, metrics
├── rules/         # Violation detection engine
├── report/        # Mermaid diagrams + markdown report
├── git/           # Git revision scanning for PR analysis
├── pr/            # Pull request diff analysis
├── snapshot/      # Run snapshots and drift comparison
├── ai/            # Groq / Gemini clients
└── config/        # .env loader

action/            # GitHub Action (composite)
.github/workflows/ # CI for this repo
```

---

## Development

```bash
mvn test          # run unit tests
mvn package       # build shaded JAR
```

---

## Design philosophy

Most coding assistants work at the **file level**. Archlytics works at the **system level**:

| Coding assistant | Archlytics |
|------------------|------------|
| Edits a function | Maps module boundaries |
| Fixes a bug | Detects architectural drift |
| Generates code | Generates architecture diagrams |
| Local context | Whole-repo dependency graph |

**Deterministic parsing builds the facts. AI interprets them.** That separation keeps findings auditable and reduces hallucination.

---

## License

MIT
