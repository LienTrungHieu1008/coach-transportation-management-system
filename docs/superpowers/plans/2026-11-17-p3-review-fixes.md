# P3 review fixes — implementation and handoff

Scope authorized by leader: update PR #6 on `feat/p3-trip-postgres-adapter`, preserve Phương's history; no merge. Contract/API/schema/dependencies/security/CI unchanged.

- [ ] Add regression: missing B–C fare while querying A–B must produce ESSENTIAL_DATA_INCOMPLETE; complete A–B/B–C must still succeed. Run real PostgreSQL verify and record RED.
- [ ] Change only fare completeness loop in PostgresTripQueryAdapter to cover all consecutive TripStops, not selected Segment. Run full verify, record GREEN.
- [ ] Add a separate snapshot IT using a committed test fixture, REPEATABLE_READ read-only caller and a writer on a separate thread/connection. Synchronize with CountDownLatch/Future (bounded timeouts, no sleeps); assert old snapshot before/after writer commit and new data in a new transaction. Restore changed fixture data even on assertion failure.
- [ ] Validate the snapshot test catches a weaker READ_COMMITTED test caller as a temporary negative control, then restore REPEATABLE_READ; do not change production isolation policy.
- [ ] Full backend verify on Java 21/PostgreSQL 16; cleanup test Compose. Keep commits small and explain each in a handoff document for Phương/Huy; push to existing PR and inspect CI. Member review is not replaced by agent verification.

Files: adapter; existing adapter IT; new snapshot IT and test fixture cleanup SQL; verification/handoff docs. No new application dependency. Existing checkout is clean and stays on the PR branch for the leader's IDE.

Execution evidence: fare RED reproduced (12 unit pass, 77 IT with 1 failure). Production loop now covers all Trip stops; fare fix commit 3504835. Snapshot READ_COMMITTED negative control failed at the intended post-commit label assertion; restored REPEATABLE_READ. Final local full verify blocked by Docker daemon timeout before Maven started; final GREEN must come from the unchanged GitHub CI verify. Do not infer GREEN from stale local XML.
