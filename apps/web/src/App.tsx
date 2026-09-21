import { useEffect, useState } from "react";
import {
  ApiError,
  type Dashboard,
  type Session,
  type Workspace,
  getJson,
} from "./api";
import "./styles.css";

type ViewState =
  | { status: "loading" }
  | { status: "anonymous" }
  | { status: "error"; message: string }
  | {
      status: "authenticated";
      session: Session;
      workspaces: Workspace[];
      dashboard: Dashboard;
    };

const roleLabels: Record<string, string> = {
  CONTACT_CENTRE_AGENT: "Contact centre",
  CLAIMS_OFFICER: "Claims officer",
  ASSESSOR: "Assessor",
  FRAUD_REVIEWER: "Fraud reviewer",
  APPROVER: "Approver",
  FINANCE_OPERATOR: "Finance operator",
  PROCESS_OWNER: "Process owner",
  PLATFORM_ADMIN: "Platform administrator",
};

export default function App() {
  const [state, setState] = useState<ViewState>({ status: "loading" });

  useEffect(() => {
    let active = true;
    async function load() {
      try {
        const session = await getJson<Session>("/api/session");
        const [workspaces, dashboard] = await Promise.all([
          getJson<Workspace[]>("/api/workspaces"),
          getJson<Dashboard>("/api/dashboard"),
        ]);
        if (active) {
          setState({ status: "authenticated", session, workspaces, dashboard });
        }
      } catch (error) {
        if (!active) return;
        if (error instanceof ApiError && error.status === 401) {
          setState({ status: "anonymous" });
          return;
        }
        setState({
          status: "error",
          message: error instanceof Error ? error.message : "Unable to load the workspace",
        });
      }
    }
    void load();
    return () => {
      active = false;
    };
  }, []);

  if (state.status === "loading") {
    return <StatusPanel title="Loading workspace" detail="Checking your secure session…" />;
  }
  if (state.status === "anonymous") {
    return <LoginPage />;
  }
  if (state.status === "error") {
    return <StatusPanel title="Workspace unavailable" detail={state.message} />;
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <a className="brand" href="/" aria-label="Insurance Operations home">
          <span className="brand-mark">IO</span>
          <span>
            <strong>Insurance Operations</strong>
            <small>Automation Platform</small>
          </span>
        </a>
        <div className="identity">
          <span>
            <strong>{state.session.displayName}</strong>
            <small>{state.session.roles.map(toRoleLabel).join(" · ")}</small>
          </span>
          <form action="/api/session/logout" method="post">
            <input name="_csrf" type="hidden" value={state.session.csrfToken} />
            <button className="text-button" type="submit">
              Sign out
            </button>
          </form>
        </div>
      </header>

      <main>
        <section className="hero">
          <div>
            <p className="eyebrow">Synthetic insurance operations reference</p>
            <h1>Work that remains visible when automation stops.</h1>
            <p>
              One role-aware workspace for cases, human decisions, durable workflows,
              integrations, and recovery.
            </p>
          </div>
          <div className="truth-boundary">
            <span>Reference boundary</span>
            <strong>No real customer, policy, claim, or payment data</strong>
          </div>
        </section>

        <section aria-labelledby="metrics-heading">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Today</p>
              <h2 id="metrics-heading">Operational overview</h2>
            </div>
            <span className="freshness">Synthetic data · refreshed now</span>
          </div>
          <div className="metric-grid">
            {[
              state.dashboard.openCases,
              state.dashboard.awaitingEvidence,
              state.dashboard.slaRisks,
            ].map((metric) => (
              <article className="metric-card" key={metric.label}>
                <span>{metric.label}</span>
                <strong>{metric.value}</strong>
                <small>{metric.detail}</small>
              </article>
            ))}
          </div>
        </section>

        <section className="content-grid">
          <div>
            <div className="section-heading">
              <div>
                <p className="eyebrow">Authorized tools</p>
                <h2>Your workspaces</h2>
              </div>
            </div>
            <div className="workspace-grid">
              {state.workspaces.map((workspace) => (
                <a className="workspace-card" href={workspace.path} key={workspace.id}>
                  <span className="workspace-icon" aria-hidden="true">
                    {workspace.label.slice(0, 2).toUpperCase()}
                  </span>
                  <span>
                    <strong>{workspace.label}</strong>
                    <small>{workspace.description}</small>
                  </span>
                  <span aria-hidden="true">→</span>
                </a>
              ))}
            </div>
          </div>

          <aside className="activity-panel" aria-labelledby="activity-heading">
            <p className="eyebrow">Case history</p>
            <h2 id="activity-heading">Recent activity</h2>
            <ol>
              {state.dashboard.recentActivity.map((activity) => (
                <li key={`${activity.caseReference}-${activity.summary}`}>
                  <span>
                    <strong>{activity.caseReference}</strong>
                    <small>{activity.summary}</small>
                  </span>
                  <time>{activity.age}</time>
                </li>
              ))}
            </ol>
          </aside>
        </section>
      </main>
    </div>
  );
}

function LoginPage() {
  return (
    <main className="login-page">
      <section className="login-copy">
        <p className="eyebrow">Open reference implementation</p>
        <h1>Governed automation for insurance operations.</h1>
        <p>
          Inspect how case work, durable workflows, human decisions, integration failures,
          and recovery fit behind one secure application.
        </p>
        <ul>
          <li>Server-enforced roles and financial authority</li>
          <li>Temporal workflows with human task signals</li>
          <li>Bounded RPA and legacy-system adapters</li>
        </ul>
      </section>
      <section className="login-card" aria-labelledby="login-heading">
        <span className="brand-mark">IO</span>
        <p className="eyebrow">Secure workspace</p>
        <h2 id="login-heading">Sign in to continue</h2>
        <p>Authentication is handled by the configured OpenID Connect provider.</p>
        <a className="primary-button" href="/oauth2/authorization/keycloak">
          Sign in with identity provider
        </a>
        <small>Local accounts use synthetic identities and roles.</small>
      </section>
    </main>
  );
}

function StatusPanel({ title, detail }: { title: string; detail: string }) {
  return (
    <main className="status-page">
      <section className="login-card">
        <span className="brand-mark">IO</span>
        <h1>{title}</h1>
        <p>{detail}</p>
      </section>
    </main>
  );
}

function toRoleLabel(role: string): string {
  return roleLabels[role] ?? role.toLowerCase().replaceAll("_", " ");
}
