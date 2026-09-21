from __future__ import annotations

import json
import time
from dataclasses import dataclass
from typing import Protocol
from urllib.parse import urlencode
from urllib.request import Request, urlopen


class TokenProvider(Protocol):
    def access_token(self) -> str: ...


@dataclass(frozen=True)
class StaticTokenProvider:
    token: str

    def access_token(self) -> str:
        return self.token


class ClientCredentialsTokenProvider:
    def __init__(self, token_url: str, client_id: str, client_secret: str) -> None:
        self._token_url = token_url
        self._client_id = client_id
        self._client_secret = client_secret
        self._token: str | None = None
        self._expires_at = 0.0

    def access_token(self) -> str:
        now = time.monotonic()
        if self._token is not None and now < self._expires_at:
            return self._token

        body = urlencode(
            {
                "grant_type": "client_credentials",
                "client_id": self._client_id,
                "client_secret": self._client_secret,
            }
        ).encode()
        request = Request(  # noqa: S310
            self._token_url,
            data=body,
            headers={
                "Accept": "application/json",
                "Content-Type": "application/x-www-form-urlencoded",
            },
            method="POST",
        )
        with urlopen(request, timeout=15) as response:  # noqa: S310
            payload = json.loads(response.read())

        token = payload.get("access_token") if isinstance(payload, dict) else None
        expires_in = payload.get("expires_in", 60) if isinstance(payload, dict) else 60
        if not isinstance(token, str) or not token:
            raise RuntimeError("Identity provider did not return an access token")
        if not isinstance(expires_in, int | float):
            expires_in = 60

        self._token = token
        self._expires_at = now + max(float(expires_in) - 15, 1)
        return token
