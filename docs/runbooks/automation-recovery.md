# Automation Work Recovery

Automation work is durable in PostgreSQL. A worker crash must not require an operator to recreate the business request.

## Procedure

1. Find the work item by business reference and inspect its status, attempts, lease owner, and lease expiry.
2. For `LEASED` work, wait for lease expiry unless the worker identity is confirmed dead.
3. Polling recovers an expired lease to `READY`; the next claim increments the attempt counter and creates a new lease token.
4. A failed attempt is rescheduled with bounded exponential delay.
5. `DEAD_LETTER` is terminal in the reference implementation. Investigate the payload and dependency failure before using an insurer-specific replay control.
6. Never reuse an old lease token and never edit a completed item.

## Verification

Run `scripts/run-recovery-verification.sh`. Its queue tests cover lease expiry, retry, completion-token validation, and dead-letter behavior. Confirm the automation dashboard shows the corresponding bounded outcome labels.
