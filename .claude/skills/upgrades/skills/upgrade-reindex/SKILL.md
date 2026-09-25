---

name: upgrade-reindex
description: Run this skill when Phase 4 (Check Reindex) of the upgrade playbook executes, or when the user invokes `/upgrade-reindex` directly. Triggers a general search-engine reindex on the upgraded portal through the `index.on.startup` portal property, armed for a single boot, and evaluates the reindex log to confirm every indexer rebuilds cleanly. No admin login, UI, or Gogo command is needed. The Search Admin "Reindex all" button, driven by a human or Playwright MCP, stays as the fallback for a full index drop or the index reindexers. Trigger also when the user says "run the reindex", "check the reindex", or "rebuild the search index".

---

# upgrade-reindex (Phase 4 — Check Reindex)

Triggers a **general reindex** of the search engine and **evaluates the log** to confirm the search
index rebuilds cleanly after the upgrade.

## Scope boundary

- Search-engine (Elasticsearch / OpenSearch) reindex only — **NOT** a database operation.
- Connecting to the *right* Elasticsearch (configured container vs. embedded sidecar) is already
  handled in **Phase 3 (Fix Startup)**. This phase assumes the portal is up and bound to the
  configured ES container.

## Prerequisites

- Phase 3 (Fix Startup) is `complete` (`upgrade-startup`): portal boots clean, all bundles `Active`,
  bound to the configured ES container, DXP license registered, login page renders. **No admin
  credential is needed** — the reindex is triggered by a portal property, not through the UI.

## Gotchas

- A reindex on an **empty / seed dataset** completes in sub-second and proves only the **trigger
  path** — not real indexing. The DB-rooted-error autonomy boundary below is exercised only when there
  is **actual content** to index. On a populated upgrade, expect per-indexer progress in the log and
  budget time accordingly — that is where data-integrity errors surface.
- **`index.on.startup` reindexes on every boot while it is set**, and `portal.properties` forbids it
  on production systems. Arm it for the one boot of this phase, in the uncommitted derived compose
  file only, and disarm it before the phase ends. It never goes into the committed compose file,
  `portal-ext.properties`, or anything that ships to the customer.
- **Tasks keep going after an error.** `ReindexSingleIndexerBackgroundTaskExecutor` logs an exception
  at `ERROR` and still prints its `Finished` line. The criterion is **no ERROR between Start and
  Finished**, never the task status or the presence of `Finished`.
- **Indexers register late.** The workspace's own Service Builder modules deploy after the portal
  boots, and the live tracker reindexes them when they register. Wait for a quiet window after the
  last `Start` line before judging the run.
- **`index.on.upgrade` no longer exists.** It is listed among obsolete keys in
  `PreupgradeVerifyProperties`. Do not use it.

## Mechanism — `index.on.startup`, armed for one boot

There is no REST, JSONWS, or Gogo reindex command, and driving the Search Admin UI headless is
unreliable on 2026.q1 (forced first-login password reset, captcha, JS-gated forms). The portal ships a
property for exactly this situation: `index.on.startup`, documented in `portal.properties` as existing
"so that automated test environments index on startup".

Verified against the 2026.q1 source (`IndexOnStartupExecutor` in portal-search): when the property is
true, the portal waits `index.on.startup.delay` seconds (default 60), then registers a portal-instance
lifecycle listener for every `Indexer`, excluding base searchers and indexers that opt out. Existing
companies are replayed into each new listener, and indexers that register later are picked up live.
Each (indexer, company) pair calls `IndexWriterHelper.reindex` with the default user, the job name
`reindexOnActivate#<className>`, and the execution mode from `ReindexConfiguration` (default `full`).
That runs `ReindexSingleIndexerBackgroundTaskExecutor`: initialize the company index if missing,
delete that class's documents, `reindexCompany`. No user session, no UI, no Gogo.

Each task logs one pair of lines:

```
Start reindexing company <companyId> for class name <className> with execution mode full
Finished reindexing company <companyId> for class name <className> with execution mode full
```

Set the property through environment variables, which every Liferay 7.3+ portal reads, Docker or not:

```
LIFERAY_INDEX_PERIOD_ON_PERIOD_STARTUP=true
LIFERAY_INDEX_PERIOD_ON_PERIOD_STARTUP_PERIOD_DELAY=15
```

### What the startup path does not do

The Search Admin **Reindex all** button fires one `ReindexPortalBackgroundTaskExecutor` per company —
in `full` mode it drops and recreates the company index and runs every indexer inside one task — plus
a `ReindexIndexReindexerBackgroundTaskExecutor` for the `IndexReindexer`s (synonym sets, rankings).
The startup path differs in two ways:

- It does **not** drop the company index, so documents of classes that no longer exist are not
  removed. Irrelevant when the Elasticsearch container is new, which is the normal case (the indices
  are created at boot). When the stack reuses an Elasticsearch data volume from the source version,
  flag it as non-blocking: "run Reindex all from Search Administration to recreate the index".
- It does **not** run the `IndexReindexer`s. Flag "rebuild synonym sets and rankings from Search
  Administration" as non-blocking for a human.

### Fallback — the Search Admin UI

Keep the UI path for when a human wants the full "Reindex all" (index drop plus `IndexReindexer`s),
or when the property cannot be set on the stack. Control Panel → Search Administration →
**Reindex all**, driven by Playwright MCP or by a human. It needs an authenticated omniadmin, and the
first-login reset is a credential change — **confirm before logging in or resetting**. Act only on the
reindex control, and capture before/after screenshots. Its two executors log the same Start/Finished
pairs, without a class name for the portal executor. Never post the portlet action by hand and never
use the Script Console: both are captcha- and JS-gated.

## Workflow

1. **Confirm Phase 3** — portal up, every bundle `Active`, bound to the configured ES container,
   license registered (no redirect to `/c/portal/license_activation`). If not, stop and return to
   Phase 3.
2. **Reuse Phase 3's stack.** Use the uncommitted derived compose file and compose project Phase 3
   booted (remapped host ports, reduced heaps). Never edit the committed `docker-compose.yaml` or
   `portal-ext.properties`. On a non-Docker workspace the two variables are exported to the Tomcat
   process instead.
3. **ES index cleanup (inherited from the removed startup C4.b)** — if any non-blocking Elasticsearch
   index cleanup is needed, do it here (ES is this phase's domain). Config / index level only — **no
   database writes**.
4. **Arm the property.** Add the two environment variables to the portal service in the derived
   compose file, then recreate only the portal service:

   ```bash
   docker compose -f <compose-file> -f <derived-file> -p <project> up -d --no-deps --force-recreate <portal-service>
   ```

   Note the boot timestamp; everything before it in the log belongs to Phase 3.
5. **Tail and evaluate the reindex log.** The run is finished when every `Start reindexing company`
   line has its matching `Finished reindexing company` line and no new `Start` line has appeared for
   60 seconds. Then, in the slice between the first `Start` and the last `Finished`:
   - no `ERROR` lines;
   - the class names include the workspace's own indexers (the Service Builder entities, custom
     indexers), one pair per company.

   Save the slice as `reindex.log` under the artifact directory, with a per-class table (class name,
   company, duration, errors).
6. **Disarm the property.** Remove the two variables from the derived compose file and recreate the
   portal service again, or stop the stack. Either way, the next boot must not reindex. Confirm the
   derived file is not staged.
7. **Restore** any temporary Elasticsearch or deploy changes made in step 3.

### Checklist

Copy into `upgrade-notes/<run-id>/phase-4-reindex/summary.md`.

- [ ] Phase 3 confirmed (bundles Active, ES container bound, license registered)
- [ ] `index.on.startup` armed in the uncommitted derived compose file only; portal recreated once
- [ ] Every `Start` line has its `Finished` line, quiet for 60 seconds, no `ERROR` in the slice
- [ ] The workspace's own indexers appear in the class list
- [ ] Any DB-rooted error recorded as suggested investigative SQL — **none executed**
- [ ] Property disarmed; derived file unstaged; nothing committed
- [ ] Report states the trigger channel (property / UI), that the `IndexReindexer`s did not run, and
      whether the run was against real data or an empty/seed dataset

## Autonomy boundaries

- **Autonomous:** arming and disarming the property in the derived compose file; recreating the portal
  service; tailing/evaluating the log; **non-database** fixes (ES config/connection, OSGi, indexer
  wiring) + restart + re-verify; ES index cleanup in step 3 (config/index level).
- **Database-rooted errors** (data integrity, orphaned references surfaced during reindex) → **never**
  auto-fix; **suggest investigative SQL queries** for the user / DBA instead.
- **Confirm first:** the UI fallback (logging in as omniadmin and clicking "Reindex all"); completing
  any pending admin password reset (a credential change).
- **Never:** execute SQL or any database write; set `index.on.startup` in the committed compose file,
  `portal-ext.properties`, or any file that ships to the customer; leave the property armed after the
  phase; reset the admin password silently; declare Phase 4 complete on an empty-dataset run without
  saying so in the report.

## Output

- Reindex outcome (clean / errors), the evaluated log slice, and the per-class table under
  `.claude/upgrade-artifacts/<run-id>/phase-4-reindex/`; the trigger channel used.
- Any DB-rooted errors listed as **suggested investigative SQL** (not executed).
- Whether validation was against **real data** or **empty/seed data**.
- Non-blocking flags: `IndexReindexer`s not run; omniadmin login and Search Administration carried
  from Phase 3 to Phase 5; index recreation when an Elasticsearch data volume was reused.
- Next: `/upgrade-phase 5` (Fix Frontend — **deferred**).
