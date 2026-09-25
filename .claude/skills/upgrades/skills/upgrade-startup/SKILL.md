---

name: upgrade-startup
description: Run this skill when Phase 3 (Fix Startup) of the upgrade playbook executes, or when the user invokes `/upgrade-startup` directly. After a DB-readiness gate and a config/properties migration, it validates the OSGi lifecycle of every custom module against Hypersonic (no database, no license needed), then guides the two-phase startup validation: first the Catalina phase (portal up without modules — fixing portal/SQL config errors and verifying the portal connects to the configured Elasticsearch container rather than the embedded sidecar), then the Startup phase (BND bundle-version fixes, deploy modules and themes, fix OSGi unresolved requirements and unsatisfied Declarative Services). The agent never upgrades the database schema. Trigger also when the user says "run the startup phase", "fix startup errors", or "deploy the modules".

---

# upgrade-startup

Validates the upgraded portal through two sequential phases — **Catalina** (portal + DB + Elasticsearch,
no modules deployed) then **Startup** (deploy the fixed modules and themes). Each has a clear boundary:
do not mix them.

## Prerequisites

- Phase 2 (`upgrade-compile`) is `complete` in `upgrade-state.md` (the workspace compiles).
- The Startup Game Plan order is recorded in `upgrade-state.md` (produced by `/upgrade-analyzer` in Phase 2): Exporters → Services/APIs → Plugins → Fragment-Hosts → Others.
- Working directory is the workspace root.
- **The DXP license gates only the checks that need HTTP.** Scan the deploy overlay that maps to `/mnt/liferay/files/deploy` for an `.xml` whose root element is `<license>` or `<licenses>` and whose `<product-version>` is on the target line. Phase 1 (`upgrade-setup-version` Step 2c) places it, or flags it for this phase when it cannot. Record the result in `upgrade-state.md`. The license is **not** needed for Step C2b: an unlicensed DXP still boots the OSGi framework and deploys modules, and only redirects web requests to `/c/portal/license_activation`. It **is** needed for C4.e and S6. When none is present, run everything up to and including C2b, then skip C4.e and S6, record `[not applied — resolve]` as **blocking** under **Flagged for human review**, naming the deploy overlay and the target line, and mark Phase 3 `blocked`. Skip this check for Portal CE targets.
- **The agent does no database work.** It may *start* the DB container (per the readiness gate below) but never runs the DB upgrade tool nor writes to the database.

## Commit convention

All commits follow the `/commit` skill and `.claude/rules/commit.md`: when no ticket applies, prefix the title with `NOISSUE`. Each step below specifies a **message hint**.

---

## Gotchas

- **Equinox caches resolution state, and `docker compose restart` does not clear it.** After a bundle
  reaches `Installed`, redeploying a fixed JAR does **not** reliably pick up the new manifest — the
  prior result is reused from `/opt/liferay/osgi/state`, inside the container filesystem, not a volume.
- **Gogo over a pipe needs the connection held open.** Piping a command into the shell and closing
  stdin right away drops the connection before the output comes back, and the result looks empty rather
  than failed. Keep the input open for a few seconds after the last command, as `## Gogo Shell` shows.
  The Gogo Shell listens on `localhost:11311` **inside** the portal container, so run it there. Treat an
  empty answer as "no data", never as "healthy", and cross-check against the portal log, where a bundle
  is healthy when its last lifecycle event is `STARTED`.
- **bnd no longer processes inherited DS annotations**, so Service Builder `*ServiceImpl` `@Reference`
  setters inherited from generated bases come up `null` and land in `ds:unsatisfied` (S0b).
- **A missing theme never blocks login** — `ThemeLocalService.getTheme()` auto-falls-back to Classic
  with only a WARN. Only a **deployed** theme that throws at render blocks it, and then the UI can't be
  reached to fix it (S6).
- **A fresh DXP bundle forces a first-login password reset** for the default admin, so the credential
  in `CLAUDE.md` may be stale. That reset is a credential change — confirm before doing it, never
  silently. Neither this phase nor Phase 4 needs an admin login (Phase 4 reindexes through the
  `index.on.startup` property), so the reset is never on the critical path: S6 only flags it for
  Phase 5.

## Reading CLAUDE.md fields

| Field | Used for | Fallback |
|---|---|---|
| `portal.start.command` | Start portal service | `blade server run` or `docker compose up -d` |
| `portal.stop.command` | Stop portal service | `docker compose down` |
| `portal.deploy.folder` | Where to copy/deploy built JARs | Ask the user |
| `docker.compose.file` | Docker compose file path | Auto-detect |
| `docker.services.portal` | Portal service name | `liferay` |
| `docker.services.database` | DB service name | `database` |
| `docker.services.search` | Elasticsearch/OpenSearch service name | `search` |
| `runtime.docs` | README path with startup instructions | `README.md` |

---

## Gogo Shell

Run Gogo commands headless by piping them into the shell and holding the connection open until the
output arrives:

```bash
gogo() {
	docker compose exec -T <portal-service> sh -c \
		"{ printf '%s\n' \"\$1\"; sleep 5; printf 'disconnect\n'; } | telnet localhost 11311" \
		gogo "$1"
}

gogo 'lb -s | grep -v Active'
gogo 'ds:unsatisfied'
gogo 'scr:info <component-name>'
gogo 'diag <bundle-id>'
```

On a non-Docker workspace, drop the `docker compose exec` wrapper and pipe into `telnet localhost 11311`
directly, or use `blade sh '<command>'`. Raise the `sleep` when a command such as `lb` returns a long
list. Save every answer under the phase's artifact directory, so the summary can quote it.

| Command | Tells you |
|---|---|
| `lb -s \| grep -v Active` | Bundles stuck in `Installed` or `Resolved` |
| `diag <bundle-id>` | The unresolved requirement that keeps a bundle `Installed` |
| `ds:unsatisfied` | Components with unsatisfied references, per bundle |
| `scr:info <component-name>` | Each reference of one component, its target filter, and whether it is bound |
| `services <interface>` | Whether any bundle registers the service a reference needs |

---

## Phase start — DB-readiness gate and config migration

Run these **before** bringing the portal up (before PART 1).

### Step 0 — DB-readiness gate

Ask the user which path applies, then proceed. **Headless default:** when nobody answers, and nothing
in `upgrade-state.md` or the run settings says the upgraded dump is in place, take the **Hypersonic**
path: run Step C2b only, and record the DB-backed boot under **Flagged for human review** as
**non-blocking** (`[not applied — resolve] Phase 3: validate the boot against the upgraded database`).
Never block Phase 3 on this question.

1. **DB already upgraded** — the schema is updated (done outside the agent) and the DB container is up with the dump imported. The agent confirms the container is reachable, then validates startup against it.
2. **Clean / empty DB** — bring the portal up against a fresh/empty DB to validate Catalina and then the modules, with no customer data. (Step C2b's HSQLDB pass is the no-container variant of this.)

Record the chosen path in `upgrade-state.md`.

### Step 1 — Config & properties migration

Migrate runtime configuration before the Catalina bring-up:

- `portal-ext.properties` — remove/rename properties invalid in the target version; reconcile against the target's documented property changes.
- OSGi `.config` / `.cfg` files under `osgi/configs/` — update factory PIDs / property names that changed.
- Environment-specific config the workspace carries (mail, clustering, etc.) — align to the target.

Commit. **Message hint:** `Migrate portal-ext and OSGi config for <target-version>`

---

## Verification loop

Both parts iterate the same way — fix one thing, re-observe, and only then move on. Never batch fixes
and re-check once: a second error can mask the first still being unresolved.

- **Catalina (C4):** stop the portal → apply **one** fix → restart (C3) → re-read the log. **Exit
  condition:** no blocking `SEVERE` remains; environment-specific warnings are logged, not fixed.
- **Startup (S3/S4):** edit `bnd.bnd` or `build.gradle` → rebuild the module → redeploy → clear the
  cached resolution (`refresh`, or recreate the container — see `## Gotchas`) → re-check. **Exit
  condition:** every bundle's last lifecycle event is `STARTED`, with no `unsatisfied` /
  `Unresolved requirement` / SCR / `FrameworkEvent ERROR` lines after your fixes.

Read state from the **portal log**, not Gogo (see `## Gotchas`).

## PART 1 — Catalina Phase

### Step C1 — Remove hotfixes

Scan for hotfix artifacts in the workspace. Common locations: `patching/` directories inside the bundle, files matching `liferay-hotfix-*.zip` or `dxp-*.lpkg`.

If any hotfix files are found, remove them entirely (including their parent `patching/` folder if it exists and becomes empty). Hotfixes are version-specific and will fail or cause errors on the upgraded portal.

Commit. **Message hint:** `Remove pre-upgrade hotfixes`

---

### Step C2 — Verify SQL driver location

Check that the database JDBC driver JAR is in the correct Tomcat path:

```
<bundle>/tomcat/webapps/ROOT/WEB-INF/shielded-container-lib/
```

**Not** in `tomcat/lib/ext/` — older Liferay versions kept it there but 7.4+ requires it in `shielded-container-lib`.

If the driver is in `tomcat/lib/ext/` only, move it to `shielded-container-lib/`. If it is already in the correct location, skip this step.

For SQL Server / Oracle: confirm the driver jar is present — it was added/updated during **Phase 1** (`upgrade-setup-version` Step 2b). Phase 3 only **verifies** the path here; if the jar is missing, flag it back to Phase 1 rather than adding it now.

---

### Step C2b — OSGi validation pass on Hypersonic

**Always run this pass**, before any DB-backed boot. It validates the OSGi lifecycle of every custom
module with no database container and no license. The portal boots **without** a configured database,
and Liferay falls back to Hypersonic (HSQLDB) when no `jdbc.default.*` configuration is found. Module
deploy and bundle resolution happen the same way; only company init and DB-touching upgrade processes
log expected errors that are safe to ignore in this pass.

Keep the edits below out of the phase's commits. They exist only for this pass, and are restored
before the next boot.

In a Docker-based workspace, comment out (or remove) two things in `docker-compose.yml`:

- The `LIFERAY_JDBC_PERIOD_DEFAULT_PERIOD_*` env vars on the liferay service.
- The `depends_on: <db-service>` entry under the liferay service.

Then start only the search and liferay services — leave the database service stopped:

```bash
docker compose up -d --no-deps search liferay
```

With the portal up, run Steps S0 to S5 against it: build, deploy in the Startup Game Plan order, fix
`Import-Package` and `ds:unsatisfied`, and confirm every bundle is `Active`. For each deployed bundle,
check in this order:

1. **Lifecycle, from the portal log.** Its last event is `STARTED`, with no `FrameworkEvent ERROR`,
   `Unresolved requirement`, or `BundleException` for it.

1. **Resolution, from Gogo.** `lb -s | grep -v Active` lists nothing of the workspace's own; for any
   bundle it lists, `diag <bundle-id>` names the missing requirement (S3).

1. **Services, from Gogo.** `ds:unsatisfied` lists nothing of the workspace's own; for any component it
   lists, `scr:info <component-name>` names the unbound reference, and `services <interface>` shows
   whether anything provides it (S4).

Without a license, web requests redirect to `/c/portal/license_activation`. That is expected in this
pass and is not an OSGi issue.

On a non-Docker workspace, edit `portal-ext.properties` to comment out the `jdbc.default.*` lines and start the portal normally. Liferay's setup wizard fallback is what activates HSQLDB.

Expected log noise that is **not** an OSGi issue and can be ignored in this pass:

- `Create <module-specific> structures upgrade process is running before default company is created`
- `Unable to get default company ID`
- `NoSuchCompanyException`
- Module-specific upgrade processes that read from `Company` or `Group`

The goal of this pass is exclusively to validate that all custom modules pass OSGi resolution. The
fixes it produces (bnd, Gradle, Java) are real and are committed per S3 and S4. After the pass
succeeds, stop the portal and restore the JDBC env vars and the database `depends_on`. Then:

- When the readiness gate selected an **already-upgraded DB with the dump imported**, and the license
  is present, boot against it via Step C3 and continue with the Catalina phase.

- Otherwise, stop here: Phase 3 ends with the Hypersonic results, per `## Phase completion`.

---

### Step C3 — Start portal (no modules)

**Do not deploy any modules or themes yet.**

Start only the portal, database, and search services using `portal.start.command` from CLAUDE.md. If the README documents a specific startup procedure, follow it.

```bash
# Docker compose example:
docker compose up -d <db-service> <search-service> <portal-service>

# Blade example:
blade server run
```

---

### Step C4 — Monitor Catalina log and fix errors

Stream the portal log and look for `SEVERE` and `ERROR` level entries during Liferay Portal initialization. Fix errors one by one in order of appearance.

Work one error at a time, per `## Verification loop`.

**Read `references/catalina-errors.md` before fixing** — it carries the per-symptom entries:

| Symptom in the trace | Entry |
|---|---|
| `ServletContainerInitializer processing` NPE on `FilterRegistration$Dynamic` | C4.a — outdated `web.xml` |
| bound to the embedded sidecar instead of the configured container | C4.b — Elasticsearch connection (es8 PID, `productionModeEnabled`) |
| LDAP / external customer host unreachable | C4.c — environment-specific; log and continue |
| anything else `SEVERE` / `ERROR` | C4.d — the general ladder |

---

#### C4.e — DXP license registered (Catalina gate)

For **DXP** targets, confirm the developer activation key placed during **Phase 1 (environment setup)**
was registered during boot — without it the portal redirects everything to
`/c/portal/license_activation` and is unusable.

- Log shows `License registered for DXP Development` (or equivalent) during startup.
- Guest requests are **not** redirected to license activation, e.g.:

  ```bash
  curl -s -o /dev/null -w '%{http_code} %{redirect_url}\n' http://localhost:8080/c/portal/login
  # expect 200/302 to /sign-in — NOT a redirect to /c/portal/license_activation
  ```

If the portal is stuck on `license_activation`, the key is not being picked up. Verify the `.xml`
(root `<license>`) sits in the **deploy overlay that maps into the container** (the `/mnt/liferay/files`
mount's `deploy/`, not the gitignored `bundles/`) and that file-install logged it. There is **no
property to disable the license** — do not improvise a bypass; fix the placement and restart.
(CE/portal targets: skip.)

---

### Step C5 — Catalina phase complete

Portal initializes without blocking `SEVERE` errors. Stop the portal before proceeding to Part 2.

Update `upgrade-state.md`:

```
Catalina phase: complete
Warnings (environment-specific, not blocking): <list>
Blocking issues: none / <describe>
```

---

## PART 2 — Startup Phase (module deployment)

### Step S0 — BND bundle-version constraints

`bnd.bnd` does not affect compilation (so Phase 2 left it alone), but stale `bundle-version`
constraints surface as OSGi resolution failures at deploy. Before building, strip them
workspace-wide (skip `build/`, `.gradle/`, `.git/`):

```bash
# for each *.bnd under modules.root:
sed -E -i 's/;?bundle-version="[^"]+"//g' <file.bnd>
```

Resolve any other obviously-stale `Bundle-Version` / bundle-metadata issues per module. (Reactive
Import-Package version-constraint fixes are handled in Step S3 as they surface.)

Commit. **Message hint:** `Remove bundle-version constraints from bnd files`

### Step S0b — BND Declarative Services options (Service Builder service modules)

Mirroring Liferay's
`UpgradeBNDDeclarativeServicesCheck` (cataloged in `.claude/reference/check-classes-reference.md`):
for each `bnd.bnd` whose definitions include `Liferay-Service: true`, ensure
`-dsannotations-options: inherit` is set (add it if missing; update if a different value is present).

```bash
# modules whose bnd.bnd declares a Service Builder service:
gg -l '^Liferay-Service:\s*true' -- '**/bnd.bnd'
```

Skip modules without `Liferay-Service: true`. Commit per module (or one sweep commit if several).
**Message hint:** `Set -dsannotations-options inherit for service modules`

> The Java-side and `@Component`-property upgrade transforms (e.g. `fragment.collection.key`,
> `UpgradeJava*` annotation edits) are applied earlier by `deprecated-api-remediation` in Phase 2,
> since they are `.java` edits. This step is only for the `bnd.bnd` instruction, which Phase 2 leaves
> untouched.

---

### Step S1 — Build all modules

From the workspace root, build all modules to generate the deployable JARs:

```bash
<gw> build
```

If individual module builds are preferred (following the Game Plan order from `/upgrade-analyzer`), build them level by level:

```bash
<gw> :<module-gradle-path>:build
```

---

### Step S2 — Deploy modules and themes

Deploy modules following the **Startup Game Plan order** from `upgrade-state.md`:

1. **Exporters**

1. **Services and APIs**

1. **Plugins**

1. **Fragment-Hosts**

1. **Others (including themes)**

For each category, copy the built JARs/WARs to `portal.deploy.folder`. Start the portal if it is not already running.

Allow time for Liferay to activate each batch before deploying the next category — monitor the log for `STARTED` messages before continuing.

**When re-deploying the same bundle repeatedly**, clear the cached resolution (see `## Gotchas`):

- **Per-bundle refresh** (fastest, no restart): in Gogo, `refresh` re-resolves all known bundles against the current JAR set. Use after copying the new JAR into the deploy folder.
- **Container recreate** (clean slate): `docker compose rm -fv <portal-service> && docker compose up -d --no-deps <portal-service>` — necessary when `refresh` alone doesn't take.

---

### Step S3 — Fix Unresolved Requirement: Import-Package

**Symptom:**

```
Unresolved requirement: Import-Package: com.client.some.package
```

Investigate the cause — several scenarios are possible:

| Cause | Fix |
|---|---|
| Version constraint in `build.gradle` no longer needed | Remove the explicit version from the dependency declaration |
| Package exists in another workspace module but is not exported | Add `Export-Package: com.client.some.package` to that module's `bnd.bnd` |
| Missing `Import-Package` in `bnd.bnd` | Add the explicit import |
| Module tries to use a service it has no access to | Exclude the package using `!` syntax (see below) |

**Excluding a package from auto-import:**

Add to the affected module's `bnd.bnd`:

```properties
Import-Package:\
    !com.package.to.exclude,\
    \
    *
```

The `*` imports all other available packages. This prevents OSGi from failing on a package it cannot satisfy.

After each `bnd.bnd` or `build.gradle` change, re-verify per `## Verification loop`.

Commit per module. **Message hint:** `Fix Import-Package in {module-name} module`

---

### Step S4 — Fix ds:unsatisfied Declarative Services

**Check for unsatisfied DS components** via the Gogo Shell, as `## Gogo Shell` shows:

```bash
gogo 'ds:unsatisfied'
gogo 'scr:info <component-name>'
```

> **Cross-check against the portal log** (see `## Gotchas`): a bundle is healthy when its last
> lifecycle event is `STARTED` (none later `STOPPED`), with no `unsatisfied` / `Unresolved
> requirement` / SCR / `FrameworkEvent ERROR` lines after your fixes.

**Symptom:**

```
Bundle {id: 1512, name: com.client.ecommerce.excel.service, version: 1.0.0}
    Declarative Service {id: 6826, name: com.client.ecommerce.excel.service.impl.SomeServiceImpl, unsatisfied references:
        {name: contractLocalService, target: null}
    }
```

**Before fixing, check:**
- Is the referenced service provided by another workspace module that has **not yet been deployed**? If so, deploy that module first (following the Startup Game Plan order) — this may be a sequencing issue, not a real error.
- Is it a Liferay platform service that changed API in the target version? Check the `deprecated-api-remediation` skill for known replacements.

**Fix:** Ensure the required service is available and the bundle providing it is active. If the dependency is on an internal service from another module, ensure that module's `bnd.bnd` exports the correct package and that the module is deployed and active.

After fixing and redeploying, re-run `ds:unsatisfied` to confirm the component is now satisfied.

Commit per module. **Message hint:** `Fix DS unsatisfied reference in {module-name} module`

---

### Step S5 — Verify all bundles active

In the Gogo Shell, confirm no bundles are stuck in `Installed` or `Resolved` state (they should be `Active`):

```bash
gogo 'lb -s | grep -v Active'
```

Confirm it from the portal log as well: every deployed bundle's last lifecycle event is `STARTED`,
with no post-fix `unsatisfied` / `Unresolved requirement` / SCR / `FrameworkEvent ERROR` lines. When
Gogo answers empty, the log alone decides (see `## Gotchas`).

Investigate and fix any non-Active bundles using the same steps as S3 and S4.

---

### Step S6 — Login-render gate (Phase 3 exit)

Catalina validated the portal *boots*; this validates it *serves pages* now that modules and themes
are deployed. Phase 4 triggers the reindex through the `index.on.startup` portal property (see
`upgrade-reindex`), so **no admin login is needed to leave this phase**: the exit gate is the login
page alone.

1. **Login page renders.** GET the sign-in page → **HTTP 200 with the login-form markup**
   (`curl -sL http://localhost:8080/c/portal/login | grep -i LoginPortlet`). Use the host port the
   derived compose file maps when the pass runs on remapped ports.
   - If a **deployed** theme throws at render it blocks login (see `## Gotchas`). Neutralize at the
     **deploy/OSGi level, not via the UI**: stop/undeploy the offending theme bundle so the site falls
     back to Classic, then re-check. Record it and **flag for Phase 5** (the theme fix itself is
     frontend work). Restore is optional for a reindex run.
2. **Flag the admin login for Phase 5.** Do not authenticate. Record under **Flagged for human
   review**, as **non-blocking** `[not applied — resolve]`: "authenticate as omniadmin and open
   Control Panel → Search Administration; complete the first-login password reset first when the
   portal forces one". Phase 5 (`upgrade-frontend-flow`) owns credentials and a real browser, and
   proves the admin UI there. Never complete the reset in this phase (a credential change — see
   `## Gotchas`).

If step 1 fails and cannot be resolved, mark Phase 3 **`blocked`** and surface — do **not** declare
Phase 3 complete: a portal that cannot serve its login page is not ready for Phase 5, and the render
failure usually points at a deployed module or theme this phase owns.

> **Why the gate stops at the login page:** driving Liferay admin actions headless on 2026.q1 is
> fragile (license gate, forced password reset, captcha, JS-gated forms). Phase 4 avoids the UI
> entirely, so nothing in the unattended pipeline depends on an admin credential — see
> `upgrade-reindex`.

---

## Phase completion

Update `upgrade-state.md`, rendering `assets/phase-3-state.md.template`.

Mark `blocked` if bundles remain non-Active after exhausting fixes, if the license is missing (C4.e and
S6 cannot run), or if the login page does not render (S6 step 1). A blocked phase still reports the Hypersonic
pass in full: every bundle's final state, and every `Import-Package` and `ds:unsatisfied` fix it made.

Report to the user per `assets/phase-3-report.txt.template`.

---

## Autonomy

- **Autonomous:** hotfix removal, SQL driver check, the Hypersonic OSGi pass (Step C2b, including its uncommitted compose edits), log monitoring, Gogo Shell queries, bnd.bnd and build.gradle edits, committing fixes; the license-registered check (C4.e); login-render check and **deploy/OSGi-level theme neutralization** in the S6 gate (config/deploy, no DB).
- **Requires user input:** any Catalina error with no documented solution (pause and describe); environment-specific issues that require access the agent cannot provide.
- **Never:** deploy modules against the configured database before the Catalina phase is clean (the Hypersonic pass in C2b is the exception); commit the C2b compose edits; skip the `ds:unsatisfied` check; **declare Phase 3 complete without the S6 login-render gate passing**; authenticate as the admin or reset its password in this phase (flag both for Phase 5); improvise a license bypass (there is none).