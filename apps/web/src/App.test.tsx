import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("App", () => {
  it("offers OIDC login when no authenticated session exists", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(null, { status: 401 }));

    render(<App />);

    expect(await screen.findByRole("heading", { name: "Sign in to continue" })).toBeVisible();
    expect(screen.getByRole("link", { name: "Sign in with identity provider" })).toHaveAttribute(
      "href",
      "/oauth2/authorization/keycloak",
    );
  });

  it("renders only workspaces returned by the authorized server response", async () => {
    vi.spyOn(globalThis, "fetch").mockImplementation(async (input) => {
      const path = input.toString();
      if (path === "/api/session") {
        return json({
          username: "claims.agent",
          displayName: "Amina Claims",
          roles: ["CLAIMS_OFFICER"],
          csrfToken: "csrf-token",
        });
      }
      if (path === "/api/workspaces") {
        return json([
          {
            id: "claims",
            label: "Claims workspace",
            description: "Triage and evidence",
            path: "/claims",
            roles: ["ROLE_CLAIMS_OFFICER"],
          },
        ]);
      }
      return json({
        openCases: { label: "Open cases", value: 8, detail: "Assigned work" },
        awaitingEvidence: { label: "Awaiting evidence", value: 3, detail: "Customer action" },
        slaRisks: { label: "SLA risks", value: 1, detail: "Due soon" },
        recentActivity: [],
      });
    });

    render(<App />);

    expect(await screen.findByText("Amina Claims")).toBeVisible();
    expect(screen.getByRole("link", { name: /Claims workspace/ })).toHaveAttribute(
      "href",
      "/claims",
    );
    expect(document.querySelector('input[name="_csrf"]')).toHaveValue("csrf-token");
    expect(screen.queryByText("Platform administration")).not.toBeInTheDocument();
    await waitFor(() => expect(screen.getByText("8")).toBeVisible());
  });
});

function json(body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { "Content-Type": "application/json" },
  });
}
