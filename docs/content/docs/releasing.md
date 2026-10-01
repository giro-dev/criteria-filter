---
title: Releasing
description: How the release pipeline versions, tests and publishes criteria-filter, and how the README badges are fed.
weight: 50
---

## Cutting a release

Releases are created by the **Release** workflow (`.github/workflows/release.yml`).
Run it from *Actions → Release → Run workflow* on `main`:

| Input | Default | Meaning |
|---|---|---|
| `release_version` | current version without `-SNAPSHOT` | Version to release, `MAJOR.MINOR.PATCH` |
| `next_increment` | `minor` | Part to increment for the next development `-SNAPSHOT` |

With `0.1.0-SNAPSHOT` on `main` and the defaults, the workflow:

1. Sets the version to `0.1.0` in `pom.xml`, the demo's `pom.xml` and `build.gradle`, and `docs/hugo.toml`.
2. Runs `./mvnw install` (all tests, with JaCoCo coverage) and builds the demo.
   If any test fails, the report is still uploaded but nothing is tagged or released.
3. Generates the visual report and badge data with `.github/scripts/test_report.py`
   and stores them in `docs/static/report/`.
4. Commits `release: v0.1.0`, tags it `v0.1.0`, then bumps to `0.2.0-SNAPSHOT`
   and commits `chore: prepare 0.2.0-SNAPSHOT`. Both commits and the tag are pushed to `main`.
5. Creates the GitHub release `v0.1.0` with the library jar, the zipped report and
   a per-functionality summary as release notes.
6. Redeploys the documentation site, which publishes the report at
   [`/report/`](https://giro-dev.github.io/criteria-filter/report/).

The workflow pushes to `main` with `GITHUB_TOKEN`. If `main` is protected, allow
GitHub Actions to bypass the rule (or the push step will be rejected).

## Test report

`test_report.py` reads `target/surefire-reports/TEST-*.xml` and
`target/site/jacoco/jacoco.csv` and writes:

| File | Content |
|---|---|
| `index.html` | Self-contained visual report: totals, status per functionality, line/method coverage per module, every test case |
| `summary.md` | Markdown table used as release notes and as the CI job summary |
| `report.json` | Totals in machine-readable form |
| `badges/*.json` | [shields.io endpoint](https://shields.io/badges/endpoint-badge) badge data |

Test classes are grouped into functionalities, and JaCoCo packages into modules,
in `.github/scripts/report-features.json`. A test class that is not listed appears
under **Other**, so add new test classes there.

CI runs the same script on every push and pull request, adds the summary to the
job page and uploads the report as the `test-reports` artifact.

## README badges

The release, tests and coverage badges are shields.io *endpoint* badges. shields.io
fetches the JSON published with the docs site and renders it:

```markdown
[![Release](https://img.shields.io/endpoint?url=https%3A%2F%2Fgiro-dev.github.io%2Fcriteria-filter%2Freport%2Fbadges%2Frelease.json)](https://github.com/giro-dev/criteria-filter/releases/latest)
[![Function coverage](https://img.shields.io/endpoint?url=https%3A%2F%2Fgiro-dev.github.io%2Fcriteria-filter%2Freport%2Fbadges%2Ffunction-coverage.json)](https://giro-dev.github.io/criteria-filter/report/)
```

Available badge files: `release.json`, `tests.json`, `coverage.json` (line coverage)
and `function-coverage.json` (JaCoCo method coverage). They reflect the last
release. Static chips use `https://img.shields.io/badge/<label>-<message>-<color>`,
for example `https://img.shields.io/badge/maintained-yes-brightgreen`.
