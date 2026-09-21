# Payment Reconciliation

Use this runbook for rejected, duplicate, delayed, or conflicting synthetic payment callbacks.

## Procedure

1. Locate the instruction by provider reference.
2. Compare callback authentication, amount, currency, status, and callback fingerprint with the stored instruction.
3. Treat an identical callback fingerprint as a safe duplicate.
4. Treat a different callback after a recorded callback as a conflict; do not overwrite the original reconciliation record.
5. Do not mark payment confirmed unless the signature is valid and amount and currency match exactly.
6. Escalate mismatches for manual investigation. The reference implementation intentionally has no endpoint that forces a payment to `CONFIRMED`.

## Verification

Run `scripts/run-recovery-verification.sh`. The payment tests cover invalid signatures, exact reconciliation, duplicate callbacks, amount mismatch, and conflicting callbacks.
