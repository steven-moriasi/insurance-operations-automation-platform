# OpenShift Target

The base manifests package the stateless application workloads, route, probes, resource controls, autoscaling, disruption budgets, restricted security contexts, and ingress network policy. PostgreSQL, MySQL, Temporal, identity, object storage, and telemetry are expected to be managed platform dependencies.

Before rendering the manifests:

1. replace the example identity, Temporal, object-storage, and image values;
2. create `insurance-platform-secrets` through External Secrets, Sealed Secrets, or the OpenShift secret store, including database, OAuth client, payment callback, and object-storage credentials;
3. pin images by digest in a production overlay;
4. bind workload identities instead of long-lived object-storage keys where the target supports it;
5. verify that the platform monitoring namespace carries OpenShift's `network.openshift.io/policy-group=monitoring` label.

Render without applying:

```bash
oc kustomize deploy/openshift/base
```

Applying these manifests is not proof that the reference platform has been deployed or certified for production.
