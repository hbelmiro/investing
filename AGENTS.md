# AGENTS.md

Instructions for AI coding agents working in this codebase.

## Project Overview

Java 25 / Quarkus application for investment portfolio management. Reads portfolio data from Google Sheets, scrapes financial indicators from Fundamentus (a Brazilian investment analysis site), and renders dashboards via Dashbuilder. Supports both Brazilian and US stock markets.

Key dependencies: Quarkus 3.x, Google Sheets API, JSoup (web scraping), JavaMoney/Moneta (monetary values), Dashbuilder (dashboards), RESTEasy (REST API).

## Build & Test

```sh
mvn -B package              # build + run tests
mvn quarkus:dev             # dev mode with live reload (http://localhost:8080)
mvn package -Dnative        # native executable (requires GraalVM)
```

Tests use the `test` build profile, which swaps `DefaultGoogleSheetsClient` for `CsvGoogleSheetsClient` (reads CSV fixtures instead of calling Google Sheets). Test assertions use AssertJ.

## Architecture

```
com.hbelmiro.investing
├── api/                  # REST endpoints (FundamentusResource, DashboardResource)
├── asset/                # Asset model and lookup
├── currency/             # Currency codes (BRL, USD)
├── dividend/             # Dividend readers (BrDividendReader, UsDividendReader)
├── googlesheets/         # Google Sheets client interface + implementations
├── operation/
│   ├── reader/           # Buy/sell operation readers per market (Br/Us)
│   └── averageprice/     # Average price calculation
├── price/                # Price reading from sheets
└── utils/                # MoneyUtil and helpers
```

### Key Patterns

- **CDI scoping**: `@ApplicationScoped` for singletons, `@Dependent` for per-injection instances.
- **Build profile switching**: `@IfBuildProfile("test")` / `@UnlessBuildProfile("test")` to swap implementations (e.g., `CsvGoogleSheetsClient` vs `DefaultGoogleSheetsClient`).
- **Config properties**: MicroProfile `@ConfigProperty` for external configuration — keep sensitive values out of code.
- **Market-specific readers**: Separate `Br*` and `Us*` classes for Brazilian and US market operations and dividends.

## Domain Context

- Brazilian stock market terminology appears in code and scraped HTML (Portuguese): "Div. Yield", "LPA", "Patrim. Líq", "Nro. Ações".
- Brazilian number formatting: `1.234,56` (dot = thousands separator, comma = decimal). The `FundamentusClient` parses this format explicitly.
- Fundamentus (`fundamentus.com.br`) is the external data source — it is scraped via JSoup. HTML structure may change without notice, so scraping code is inherently fragile.
- Google Sheets stores the user's portfolio with read-only access (`SPREADSHEETS_READONLY` scope).
- Monetary values use JavaMoney/Moneta — use `MoneyUtil` for currency operations. **Always use `Money` for monetary arithmetic (prices, costs, gains, exchange rates), never extract to raw `BigDecimal` for intermediate calculations.** `BigDecimal` subtraction produces floating-point artifacts (e.g., `2.2E-16` instead of `0`); `Money` handles precision correctly. Share quantities (`BigDecimal`) are not monetary values.

## IRPF Calculation Rules (Receita Federal)

Per Receita Federal (https://www.gov.br/receitafederal/pt-br/assuntos/meu-imposto-de-renda/pagamento/renda-variavel/bolsa-de-valores-1/ganho-liquido):

- **Average cost (custo médio ponderado):** simple weighted average of ALL buys:
  `sum(buy price × buy quantity + tax) / sum(buy quantities)`. Sells do NOT affect the per-unit average.
- **Capital gains:** `(sell price × amount × PTAX venda) - (avg cost BRL × amount)`.
  The avg cost used is the buy-only weighted average.
- **Bens e Direitos:** declare remaining quantity × avg cost at 31/12.

## Security & Sensitive Data

- **Never commit credentials**: `credentials.json` and `tokens/StoredCredential` are gitignored. Never hardcode or log OAuth2 credentials.
- **Sensitive config properties**: `credentials.file-path`, `credentials.user`, and `spreadsheet-id` contain values that should not be exposed.
- **No `.env` files**: Configuration is via Quarkus `application.properties` and system properties.

## CI/CD

- **Maven CI**: Runs on pushes and PRs to `main`. Java 25, Temurin distribution, Maven caching.
- **Dependabot**: Daily Maven dependency PRs, weekly GitHub Actions PRs. Auto-merged after CI passes.
- **CodeQL**: Security analysis on pushes, PRs, and a weekly Saturday schedule. Uses `security-and-quality` query suite for Java.
