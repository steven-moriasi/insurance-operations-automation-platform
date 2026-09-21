export type Session = {
  username: string;
  displayName: string;
  roles: string[];
  csrfToken: string;
};

export type Workspace = {
  id: string;
  label: string;
  description: string;
  path: string;
  roles: string[];
};

export type Metric = {
  label: string;
  value: number;
  detail: string;
};

export type Activity = {
  caseReference: string;
  summary: string;
  age: string;
};

export type Dashboard = {
  openCases: Metric;
  awaitingEvidence: Metric;
  slaRisks: Metric;
  recentActivity: Activity[];
};

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message);
  }
}

export async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(path, {
    credentials: "same-origin",
    headers: { Accept: "application/json" },
  });
  if (!response.ok) {
    throw new ApiError(response.status, `Request failed with ${response.status}`);
  }
  return (await response.json()) as T;
}
