# OPUS — Project Notes for Agents

Multi-client app: `api/` (pure PHP REST API, no framework), `android/` (Kotlin + Jetpack Compose + Hilt), `desktop/` (Electron + React + TS + Zustand + Tailwind). Database migrations are plain SQL files in `database/`, applied in sorted order by `api/reset-db.php` (drops and recreates the DB — destructive).

## Conventions

- CRUD features follow the Correspondance reference implementation end-to-end:
  - API: `api/src/Models/Correspondance.php`, `api/src/Controllers/CorrespondanceController.php`, routes in `api/public/index.php`, migration `database/019_create_correspondance.sql`.
  - Permission module codes look like `sedentaire_secretariat_correspondance` (checked via `hasPermission(user, module, action)` on both clients; server-side only requires auth + ownership-style flows).
  - Notifications: controllers call `Notification::notifyFeatureChange($module, $adminData, $userData, $actorId)` on create/update; push delivery is isolated inside `Notification::create()` and never fails the request.
  - Audit: `AuditLog::create(...)` on every mutation.
- Tests are plain PHP scripts (no PHPUnit): `php api/tests/<Name>Test.php` — they build a scratch MySQL DB and drop it.

## Verification commands (Windows)

- API tests: `php api/tests/CorrespondanceTest.php`, `php api/tests/NotificationTest.php`, `php api/tests/DeclarationPerteTest.php`, `php api/tests/PassationTest.php`, `php api/tests/ArmementTest.php`
- Desktop: `npm.cmd run build` in `desktop/` (PowerShell blocks `npm.ps1`; use `npm.cmd`). `tsc` runs as part of build.
- Android: `gradlew.bat compileDebugKotlin testDebugUnitTest --console=plain` in `android/`. JAVA_HOME is not set globally; use `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"` (Android Studio bundled JBR).
- Apply a single new migration to the dev DB without resetting: run its CREATE/ALTER statements against the `opus` DB (see git history for `api/apply-020.php` pattern).
