# AGENTS.md

Instructions for AI coding agents (Claude Code, Codex, etc.) working in this repo.

## What this is

A prototype **online grocery substitution agent** for a fictional grocer. When an item in an
order is out of stock at picking time, the agent proposes a substitute with a one-line reason,
and a picker approves or overrides it. It is a learning project, not production software.

## Stack

- Kotlin 2.3, Java 25 toolchain, Gradle (Kotlin DSL) via `./gradlew`
- Spring Boot 4.1, Spring AI 2.0
- MCP server: `spring-ai-starter-mcp-server-webmvc`, `spring.ai.mcp.server.protocol=STREAMABLE`
- Snowflake over JDBC (`JdbcClient`); H2 in-memory database for tests only
- Gemini (gemini-3.5-flash) on Vertex AI via Spring AI's Google GenAI starter, authenticated with ADC

## Commands

- Run tests: `./gradlew test` (H2 and a fake chat model; no credentials needed)
- Run live evals: `./gradlew liveEval` (real Gemini via ADC; local only, never in CI)
- Run the app: `SPRING_PROFILES_ACTIVE=snowflake ./gradlew bootRun`

## Rules

- **Tests must pass without Snowflake credentials.** CI has none; tests use H2.
- **MCP tools are read-only and typed.** Typed parameters, named SQL parameters, row limits.
  Never build SQL by string concatenation, and never expose a free-form query tool.
- **Guardrails live in code, not prompts.** Allergen, diet and price-band rules are
  deterministic Kotlin with unit tests. The model ranks candidates; code filters them.
- **No secrets in the repo.** Credentials come from environment variables.
- **Code drives the agent loop; the model only ranks.** Code calls the tools in a fixed order and
  applies every hard rule. The model chooses among candidates that already passed, and its answer
  is re-checked in code. Never let model output select a product that hasn't passed the filter.
- **Picker decisions use the same rules as the agent.** Approve and override go through the shared
  candidate assessment; don't add a separate path.
- **Tests that call a real model are tagged `live`** and must not run in `./gradlew test`.
- **Check current docs** for versions, model names and APIs rather than relying on memory.
- Keep the scope small; see the project plan for what is out of scope.

## Working with the author

- Claude drafts the code, including the core Kotlin; the author reviews every piece before it is
  committed. Work in small steps that can be reviewed one at a time, and say what the author
  should see when they run each one.
- Comments are for a reader who already knows Kotlin: explain why, never syntax. Explanations of
  Kotlin, JVM or Spring concepts go in chat, not in the code. The author has an iOS/Swift
  background: explain by comparison with Swift, SPM and Xcode where it helps.
- Refer to products, orders and customers by name as well as ID, never by bare ID.
- Commit messages are one line. The author runs git and stages files.
- When something fails, diagnose the cause rather than adding workarounds.
