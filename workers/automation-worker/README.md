# Bounded Automation Worker

This Python 3.12 worker executes activities that need document-processing or scripted-integration tooling without making the worker authoritative for claim, approval, or payment state.

Supported work types:

- `DOCUMENT_SCAN` downloads an object through a presigned URL, enforces the registered size and content signature, checks the deterministic test malware signature, and reports the observed digest and result.
- `LEGACY_POLICY_UPDATE` simulates an allowlisted legacy policy-screen update.
- `BROKER_PORTAL_CHECK` simulates a bounded intermediary portal check.

The integration service owns leases, retries, dead-letter state, and result persistence. A worker must present the issued lease token when completing or failing a work item.

## Run locally

```bash
python -m venv .venv
. .venv/bin/activate
pip install -e '.[dev]'
ruff check .
ruff format --check .
mypy
pytest
```

The polling entry point requires:

```text
INTEGRATION_SERVICE_URL
AUTOMATION_WORKER_ACCESS_TOKEN
```

Optional settings are `AUTOMATION_WORKER_ID` and `AUTOMATION_POLL_SECONDS`. Use a short-lived OAuth access token scoped to the `AUTOMATION_WORKER` role; do not place tokens in source control.
