# Contributing

Contributions should preserve the repository's purpose as a synthetic, inspectable reference implementation.

## Ground rules

- Do not add real customer, policy, claim, payment, identity, or insurer configuration data.
- Do not describe simulated integrations as production integrations.
- Keep business rules configurable and label illustrative rules clearly.
- Add tests for changed behavior.
- Record material architecture decisions in `docs/adr`.
- Avoid coupling domain code to a specific insurer, RPA product, payment provider, or cloud service.
- Never commit credentials, tokens, private keys, or `.env` files.

## Quality checks

The root `Makefile` will provide the canonical formatting, static-analysis, test, build, and local startup commands as implementation modules are added.

Changes should keep the complete local golden path executable with synthetic data and update the relevant architecture, runbook, or threat-model documentation when boundaries change.
