from __future__ import annotations

import json
from unittest.mock import MagicMock, patch

from insurance_worker.auth import ClientCredentialsTokenProvider, StaticTokenProvider


def test_static_provider_returns_configured_token() -> None:
    assert StaticTokenProvider("reference-token").access_token() == "reference-token"


@patch("insurance_worker.auth.urlopen")
def test_client_credentials_provider_caches_access_token(urlopen: MagicMock) -> None:
    response = MagicMock()
    response.read.return_value = json.dumps(
        {"access_token": "worker-token", "expires_in": 300}
    ).encode()
    urlopen.return_value.__enter__.return_value = response
    provider = ClientCredentialsTokenProvider(
        "https://identity.example.test/token",
        "automation-worker",
        "local-secret",
    )

    assert provider.access_token() == "worker-token"
    assert provider.access_token() == "worker-token"
    assert urlopen.call_count == 1
