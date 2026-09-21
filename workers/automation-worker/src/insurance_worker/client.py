from __future__ import annotations

import json
from dataclasses import dataclass
from typing import Any, cast
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from insurance_worker.auth import TokenProvider


@dataclass(frozen=True)
class WorkItem:
    id: str
    work_type: str
    business_reference: str
    payload: dict[str, Any]
    lease_token: str


class IntegrationClient:
    def __init__(self, base_url: str, token_provider: TokenProvider, worker_id: str) -> None:
        self._base_url = base_url.rstrip("/")
        self._token_provider = token_provider
        self._worker_id = worker_id

    def claim(self) -> WorkItem | None:
        response = self._request(
            "POST",
            "/internal/api/v1/integrations/automation/worker/claim",
            headers={"X-Worker-Id": self._worker_id},
            allow_no_content=True,
        )
        if response is None:
            return None
        return WorkItem(
            id=response["id"],
            work_type=response["workType"],
            business_reference=response["businessReference"],
            payload=response["payload"],
            lease_token=response["leaseToken"],
        )

    def document_download_url(self, document_id: str) -> str:
        response = self._request(
            "GET",
            f"/internal/api/v1/integrations/documents/{document_id}/worker-download",
        )
        if response is None:
            raise RuntimeError("Document download response was empty")
        return str(response["downloadUrl"])

    def submit_document_scan(
        self,
        item: WorkItem,
        *,
        observed_sha256: str,
        observed_bytes: int,
        clean: bool,
        scanner_summary: str,
    ) -> None:
        document_id = item.payload["documentId"]
        self._request(
            "POST",
            f"/internal/api/v1/integrations/documents/{document_id}/scan-results",
            {
                "workItemId": item.id,
                "leaseToken": item.lease_token,
                "observedSha256": observed_sha256,
                "observedBytes": observed_bytes,
                "clean": clean,
                "scannerSummary": scanner_summary,
            },
        )

    def complete(self, item: WorkItem, result: dict[str, Any]) -> None:
        self._request(
            "POST",
            "/internal/api/v1/integrations/automation/worker/complete",
            {"workItemId": item.id, "leaseToken": item.lease_token, "result": result},
        )

    def fail(self, item: WorkItem, error: str) -> None:
        self._request(
            "POST",
            "/internal/api/v1/integrations/automation/worker/fail",
            {
                "workItemId": item.id,
                "leaseToken": item.lease_token,
                "error": error[:512],
            },
        )

    def _request(
        self,
        method: str,
        path: str,
        body: dict[str, Any] | None = None,
        *,
        headers: dict[str, str] | None = None,
        allow_no_content: bool = False,
    ) -> dict[str, Any] | None:
        request_headers = {
            "Authorization": f"Bearer {self._token_provider.access_token()}",
            "Accept": "application/json",
        }
        if headers:
            request_headers.update(headers)
        encoded_body = None
        if body is not None:
            request_headers["Content-Type"] = "application/json"
            encoded_body = json.dumps(body, separators=(",", ":")).encode()
        request = Request(  # noqa: S310
            f"{self._base_url}{path}",
            data=encoded_body,
            headers=request_headers,
            method=method,
        )
        try:
            with urlopen(request, timeout=30) as response:  # noqa: S310
                if response.status == 204 and allow_no_content:
                    return None
                decoded = json.loads(response.read())
                if not isinstance(decoded, dict):
                    raise RuntimeError("Integration API returned a non-object JSON response")
                return cast(dict[str, Any], decoded)
        except HTTPError as error:
            detail = error.read().decode(errors="replace")[:512]
            raise RuntimeError(f"Integration API returned HTTP {error.code}: {detail}") from error
