---
trigger: always_on
alwaysApply: true
---
## Agent skills

### Issue tracker

Issues and PRDs are tracked as GitHub Issues via the `gh` CLI. PRs are NOT a triage surface. See `docs/agents/issue-tracker.md`.

### Triage labels

Five canonical labels: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context layout — `CONTEXT.md` at repo root + `docs/adr/` for architectural decisions. See `docs/agents/domain.md`.
