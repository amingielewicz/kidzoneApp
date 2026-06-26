# Dependency Check review

Issue: #168
Date: 2026-06-27

## Scope

This project now has OWASP Dependency Check configured through Gradle and a scheduled/manual GitHub Actions workflow.

The scan covers:

- Android Gradle dependencies for `:app`,
- admin panel `package-lock.json`,
- Cloud Functions `package-lock.json`.

## Configuration

- Plugin: `org.owasp.dependencycheck` `12.2.2`.
- Task: `./gradlew dependencyCheckAnalyze`.
- Reports: HTML, JSON and SARIF.
- Build failure threshold: CVSS `9.0` and above.
- NVD access: set GitHub secret `NVD_API_KEY` for reliable scheduled scans. Without it, NVD can return `429` rate limits and the scan may not complete.
- Workflow: `.github/workflows/dependency-check.yml`.
- CI cache: `~/.gradle/dependency-check-data` is cached between workflow runs to avoid rebuilding the NVD database from scratch every time.
- CI timeout: the workflow allows up to 90 minutes because the first NVD database update can be slow.
- Diagnostics: the workflow uploads `dependency-check-gradle.log` together with HTML/JSON/SARIF reports so failed NVD runs still leave an actionable log artifact.

## Review procedure

Run the scan locally before dependency/security PRs:

```powershell
$env:NVD_API_KEY="<free NVD API key>"
.\gradlew.bat dependencyCheckAnalyze
```

If the report finds vulnerabilities:

- prioritize critical and high findings first,
- confirm whether the vulnerable component is reachable in the app,
- update one risky dependency family per PR when possible,
- rerun Android unit tests, lint and the dependency scan,
- document false positives with a suppression file only after verification.

## GitHub Actions procedure

Use the `Dependency Check` workflow manually after dependency updates. The scheduled run executes weekly and uploads reports as workflow artifacts. If SARIF is generated, it is also uploaded to GitHub code scanning. The first successful run can take much longer because it has to populate the Dependency Check database; later runs should reuse the GitHub Actions cache. Dependency Check runs are not auto-cancelled by newer runs on the same branch, because cancelling during NVD sync wastes the partially populated database.

Before closing issue #168:

- confirm that the repository secret `NVD_API_KEY` is set and accepted by NVD,
- run the `Dependency Check` workflow at least once without manual cancellation,
- download the `dependency-check-reports` artifact,
- review the HTML or JSON report for critical/high findings,
- document any required dependency update or verified false positive.

If the workflow fails before producing HTML/JSON/SARIF reports, use the uploaded `dependency-check-gradle.log` artifact to distinguish an invalid API key, NVD rate limiting, a network timeout, or a real analysis failure.

## Current result

Dependency Check is configured and the Gradle task is registered. Earlier full scans without a working `NVD_API_KEY` reached NVD but failed or timed out before producing a complete vulnerability report. The first actionable vulnerability report should be generated after adding the free `NVD_API_KEY` secret and running the `Dependency Check` workflow, or by running the command locally with that environment variable set.

Until that report exists, #168 should stay open even if the workflow configuration itself is present.
