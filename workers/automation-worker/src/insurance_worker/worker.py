from __future__ import annotations

import hashlib
from collections.abc import Callable
from typing import Any, Protocol, cast
from urllib.request import urlopen

from insurance_worker.client import WorkItem

EICAR_SIGNATURE = b"EICAR-STANDARD-ANTIVIRUS-TEST-FILE"


class WorkerClient(Protocol):
    def claim(self) -> WorkItem | None: ...

    def document_download_url(self, document_id: str) -> str: ...

    def submit_document_scan(
        self,
        item: WorkItem,
        *,
        observed_sha256: str,
        observed_bytes: int,
        clean: bool,
        scanner_summary: str,
    ) -> None: ...

    def complete(self, item: WorkItem, result: dict[str, Any]) -> None: ...

    def fail(self, item: WorkItem, error: str) -> None: ...


class BoundedAutomationWorker:
    def __init__(
        self,
        client: WorkerClient,
        downloader: Callable[[str, int], bytes] | None = None,
    ) -> None:
        self._client = client
        self._downloader = downloader or download_bounded

    def run_once(self) -> bool:
        item = self._client.claim()
        if item is None:
            return False
        try:
            if item.work_type == "DOCUMENT_SCAN":
                self._scan_document(item)
            elif item.work_type in {"LEGACY_POLICY_UPDATE", "BROKER_PORTAL_CHECK"}:
                self._run_synthetic_rpa(item)
            else:
                raise ValueError(f"Unsupported bounded activity: {item.work_type}")
        except Exception as error:
            self._client.fail(item, f"{type(error).__name__}: {error}")
        return True

    def _scan_document(self, item: WorkItem) -> None:
        maximum_bytes = int(item.payload["maximumBytes"])
        download_url = self._client.document_download_url(item.payload["documentId"])
        content = self._downloader(download_url, maximum_bytes)
        content_type_matches = matches_content_type(content, item.payload["contentType"])
        clean = EICAR_SIGNATURE not in content and content_type_matches
        summary = (
            "No known malware signature detected"
            if clean
            else "Object failed malware or content-signature validation"
        )
        self._client.submit_document_scan(
            item,
            observed_sha256=hashlib.sha256(content).hexdigest(),
            observed_bytes=len(content),
            clean=clean,
            scanner_summary=summary,
        )

    def _run_synthetic_rpa(self, item: WorkItem) -> None:
        if item.work_type == "LEGACY_POLICY_UPDATE":
            result = {
                "outcome": "APPLIED",
                "policyNumber": item.payload["policyNumber"],
                "simulated": True,
            }
        else:
            broker_reference = str(item.payload["brokerReference"])
            result = {
                "outcome": "MANUAL_REVIEW" if broker_reference.endswith("REVIEW") else "CLEAR",
                "brokerReference": broker_reference,
                "simulated": True,
            }
        self._client.complete(item, result)


def download_bounded(url: str, maximum_bytes: int) -> bytes:
    with urlopen(url, timeout=30) as response:  # noqa: S310
        content = cast(bytes, response.read(maximum_bytes + 1))
    if len(content) > maximum_bytes:
        raise ValueError("Downloaded object exceeded the registered size limit")
    return content


def matches_content_type(content: bytes, content_type: str) -> bool:
    signatures = {
        "application/pdf": (b"%PDF-",),
        "image/jpeg": (b"\xff\xd8\xff",),
        "image/png": (b"\x89PNG\r\n\x1a\n",),
    }
    return any(content.startswith(signature) for signature in signatures[content_type])
