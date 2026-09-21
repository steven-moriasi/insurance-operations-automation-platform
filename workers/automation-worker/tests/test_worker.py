from __future__ import annotations

import hashlib
from typing import Any

from insurance_worker.client import WorkItem
from insurance_worker.worker import EICAR_SIGNATURE, BoundedAutomationWorker


class FakeClient:
    def __init__(self, item: WorkItem | None) -> None:
        self.item = item
        self.completed: dict[str, Any] | None = None
        self.scan: dict[str, Any] | None = None
        self.error: str | None = None

    def claim(self) -> WorkItem | None:
        item = self.item
        self.item = None
        return item

    def document_download_url(self, document_id: str) -> str:
        return f"https://objects.example.test/{document_id}"

    def submit_document_scan(
        self,
        item: WorkItem,
        *,
        observed_sha256: str,
        observed_bytes: int,
        clean: bool,
        scanner_summary: str,
    ) -> None:
        self.scan = {
            "observed_sha256": observed_sha256,
            "observed_bytes": observed_bytes,
            "clean": clean,
            "scanner_summary": scanner_summary,
        }

    def complete(self, item: WorkItem, result: dict[str, Any]) -> None:
        self.completed = result

    def fail(self, item: WorkItem, error: str) -> None:
        self.error = error


def test_scans_a_bounded_pdf_and_reports_its_digest() -> None:
    content = b"%PDF-1.7 synthetic evidence"
    item = WorkItem(
        id="work-1",
        work_type="DOCUMENT_SCAN",
        business_reference="document-1",
        payload={
            "documentId": "document-1",
            "contentType": "application/pdf",
            "maximumBytes": 1000,
        },
        lease_token="lease-1",
    )
    client = FakeClient(item)

    processed = BoundedAutomationWorker(client, lambda url, limit: content).run_once()

    assert processed is True
    assert client.error is None
    assert client.scan == {
        "observed_sha256": hashlib.sha256(content).hexdigest(),
        "observed_bytes": len(content),
        "clean": True,
        "scanner_summary": "No known malware signature detected",
    }


def test_rejects_the_standard_antivirus_test_signature() -> None:
    content = b"%PDF-1.7 " + EICAR_SIGNATURE
    item = WorkItem(
        id="work-2",
        work_type="DOCUMENT_SCAN",
        business_reference="document-2",
        payload={
            "documentId": "document-2",
            "contentType": "application/pdf",
            "maximumBytes": 1000,
        },
        lease_token="lease-2",
    )
    client = FakeClient(item)

    BoundedAutomationWorker(client, lambda url, limit: content).run_once()

    assert client.scan is not None
    assert client.scan["clean"] is False


def test_runs_only_allowlisted_synthetic_rpa_activities() -> None:
    item = WorkItem(
        id="work-3",
        work_type="BROKER_PORTAL_CHECK",
        business_reference="BRK-10-REVIEW",
        payload={"brokerReference": "BRK-10-REVIEW"},
        lease_token="lease-3",
    )
    client = FakeClient(item)

    BoundedAutomationWorker(client).run_once()

    assert client.completed == {
        "outcome": "MANUAL_REVIEW",
        "brokerReference": "BRK-10-REVIEW",
        "simulated": True,
    }
