#!/usr/bin/env sh
set -eu

MVN="${MVN:-mvn}"
PYTHON="${PYTHON:-python3}"

"$MVN" -B -pl services/integration-service \
  -Dtest=AutomationQueueServiceTest,PaymentIntegrationServiceTest,DocumentIntegrationServiceTest \
  test

"$MVN" -B -pl services/workflow-service \
  -Dtest=MotorClaimWorkflowTest,PolicyRenewalWorkflowTest,BrokerOnboardingWorkflowTest \
  test

"$PYTHON" -m pytest workers/automation-worker/tests -q
