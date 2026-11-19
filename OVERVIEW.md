
# ReconcileHub: Automated Payment Reconciliation

## Background

ReconcileHub matches incoming payments to open invoices. The HTTP adapter is complete, but the reconciliation service behind it is not.

## Task

Register invoice batches atomically. Match each payment against unpaid invoices by memo ID, then exact amount, then amount within a tolerance, with deterministic tie-breaking. Settle each invoice at most once. Replay retries with the same idempotency key, and reject conflicting ones. Payment batches are all-or-nothing, and every operation must be thread-safe.

## What This Problem Tests

- **Input parsing** and validation
- **Tiered matching** with deterministic tie-breaking
- **Idempotency** and retry handling
- **Atomic transactions** and rollback
- **Concurrency** and thread safety
- HTTP **REST APIs** and status codes

---

## Next step

This page is a short overview. The full problem statement — detailed requirements, which files to edit, and how to run the tests — is in [README.md](./README.md). Read it before you start.
