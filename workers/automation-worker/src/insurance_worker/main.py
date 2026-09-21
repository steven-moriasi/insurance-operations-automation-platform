from __future__ import annotations

import os
import time

from insurance_worker.client import IntegrationClient
from insurance_worker.worker import BoundedAutomationWorker


def main() -> None:
    client = IntegrationClient(
        required("INTEGRATION_SERVICE_URL"),
        required("AUTOMATION_WORKER_ACCESS_TOKEN"),
        os.getenv("AUTOMATION_WORKER_ID", "python-automation-worker"),
    )
    worker = BoundedAutomationWorker(client)
    poll_seconds = float(os.getenv("AUTOMATION_POLL_SECONDS", "2"))
    while True:
        if not worker.run_once():
            time.sleep(poll_seconds)


def required(name: str) -> str:
    value = os.getenv(name)
    if not value:
        raise RuntimeError(f"{name} is required")
    return value


if __name__ == "__main__":
    main()
