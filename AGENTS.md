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

## Commands

- Run tests: `./gradlew test`
- Run the app: `./gradlew bootRun`

## Rules

- **Tests must pass without Snowflake credentials.** CI has none; tests use H2.
- **MCP tools are read-only and typed.** Typed parameters, named SQL parameters, row limits.
  Never build SQL by string concatenation, and never expose a free-form query tool.
- **Guardrails live in code, not prompts.** Allergen, diet and price-band rules are
  deterministic Kotlin with unit tests. The model ranks candidates; code filters them.
- **No secrets in the repo.** Credentials come from environment variables.
- Keep the scope small; see the project plan for what is out of scope.

## Working with the author

- The author writes the core Kotlin (MCP tools, guardrail filter, agent loop) by hand.
  For that code, review, explain and suggest idiomatic Kotlin rather than writing it.
- Boilerplate, config, build files and test scaffolding can be generated when asked.
- The author has an iOS/Swift background: explain JVM, Gradle and Spring concepts by
  comparison with Swift, SPM and Xcode where it helps.
