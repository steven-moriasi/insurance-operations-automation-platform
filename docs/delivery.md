# Delivery and Deployment Evidence

The repository packages application images, OpenShift workload definitions, AWS managed dependencies, and continuous delivery as inspectable source.

## Image contract

- Java services use one multi-stage Dockerfile parameterized by Maven module.
- The React application is served by an unprivileged Node.js edge process that proxies authenticated routes to the BFF.
- The Python worker installs only its runtime package and runs as a non-root user.
- Release workflows publish version tags and report the resulting registry digest; production overlays must promote the digest rather than redeploying a mutable tag.

## Local system contract

`docker compose up --build` starts the application and its synthetic dependencies as one system. The Compose package contains local-only credentials, an imported Keycloak realm, service identities, a synthetic legacy policy, and object-storage bootstrap. It is for development and review, not a production security model.

## OpenShift contract

The base Kustomize package defines the stateless workloads, one public route, restricted container contexts, health probes, requests and limits, autoscaling, disruption budgets, and ingress network policy. Secret values and managed dependencies are intentionally external.

## AWS contract

Terraform defines private networking, encrypted Multi-AZ PostgreSQL, evidence storage, container registries, secret containers, KMS, and log groups. It does not claim that the target has been applied.

## Pipeline contract

Every change runs Java, React, Python, container-build, OpenShift-render, and Terraform-validation jobs. A version tag builds and publishes the six application images to GHCR. Deployment promotion is deliberately separate because each adopting insurer must configure its own protected environments, approvals, identity, secrets, and image-digest policy.

Promote by digest, render the target overlay, review the diff and Terraform plan, deploy with rolling readiness gates, and retain the previously approved digest. Rollback changes only the application digest; database migrations must remain backward compatible until the prior release is outside the recovery window.

The workflows may remain unable to start while the repository owner's GitHub Actions billing restriction is active. Local verification remains the source of evidence until an Actions run is observed.
