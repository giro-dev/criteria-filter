#!/usr/bin/env python3
"""Builds a visual test/coverage report and shields.io badge data.

Reads Surefire XML reports and the JaCoCo CSV report, groups test results by
functionality (see report-features.json) and writes:

  <out>/index.html            self-contained visual report
  <out>/summary.md            Markdown summary (job summary / release notes)
  <out>/badges/*.json         shields.io endpoint badges
  <out>/report.json           machine-readable totals
"""
import argparse
import csv
import datetime
import glob
import html
import json
import os
import xml.etree.ElementTree as ET

STATUSES = ("passed", "failed", "skipped")


def parse_surefire(directory):
    cases = []
    for path in sorted(glob.glob(os.path.join(directory, "TEST-*.xml"))):
        root = ET.parse(path).getroot()
        suites = [root] if root.tag == "testsuite" else root.findall("testsuite")
        for suite in suites:
            for case in suite.findall("testcase"):
                classname = case.get("classname") or suite.get("name") or ""
                status, message = "passed", ""
                for tag in ("failure", "error"):
                    node = case.find(tag)
                    if node is not None:
                        status = "failed"
                        message = node.get("message") or next(iter((node.text or "").strip().splitlines()), "")
                if case.find("skipped") is not None:
                    status = "skipped"
                    message = case.find("skipped").get("message") or ""
                cases.append({
                    "class": classname.rsplit(".", 1)[-1],
                    "name": case.get("name") or "",
                    "time": float(case.get("time") or 0),
                    "status": status,
                    "message": message,
                })
    return cases


def parse_jacoco(path, module_names):
    modules = {}
    if not path or not os.path.exists(path):
        return modules
    with open(path, newline="") as handle:
        for row in csv.DictReader(handle):
            label = module_names.get(row["PACKAGE"], row["PACKAGE"])
            entry = modules.setdefault(label, {k: [0, 0] for k in ("line", "method", "branch")})
            for kind, column in (("line", "LINE"), ("method", "METHOD"), ("branch", "BRANCH")):
                entry[kind][0] += int(row[f"{column}_COVERED"])
                entry[kind][1] += int(row[f"{column}_COVERED"]) + int(row[f"{column}_MISSED"])
    return modules


def pct(covered, total):
    return 100.0 * covered / total if total else 0.0


def coverage_color(value):
    for threshold, color in ((90, "brightgreen"), (80, "green"), (70, "yellowgreen"), (60, "yellow"), (50, "orange")):
        if value >= threshold:
            return color
    return "red"


def group_by_feature(cases, features):
    owner = {test: feature["name"] for feature in features for test in feature["tests"]}
    groups = {feature["name"]: [] for feature in features}
    for case in cases:
        groups.setdefault(owner.get(case["class"], "Other"), []).append(case)
    result = []
    for name, items in groups.items():
        counts = {status: sum(1 for c in items if c["status"] == status) for status in STATUSES}
        status = "failed" if counts["failed"] else ("skipped" if not counts["passed"] else "passed")
        if not items:
            status = "missing"
        result.append({"name": name, "cases": items, "counts": counts, "status": status,
                       "time": sum(c["time"] for c in items)})
    return result


def badge(label, message, color):
    return {"schemaVersion": 1, "label": label, "message": message, "color": color}


CSS = """
:root{--ok:#2da44e;--fail:#cf222e;--skip:#bf8700;--muted:#57606a;--bg:#f6f8fa;--line:#d0d7de}
*{box-sizing:border-box}body{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Helvetica,Arial,sans-serif;margin:0;color:#1f2328;background:#fff}
header{background:#0d1117;color:#fff;padding:28px 40px}header h1{margin:0 0 6px;font-size:26px}header p{margin:0;color:#9da7b3}
main{max-width:1100px;margin:0 auto;padding:24px 40px 60px}h2{margin-top:36px;border-bottom:1px solid var(--line);padding-bottom:6px}
.cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(170px,1fr));gap:14px;margin-top:-44px}
.card{background:#fff;border:1px solid var(--line);border-radius:10px;padding:16px;box-shadow:0 1px 3px rgba(0,0,0,.06)}
.card .v{font-size:28px;font-weight:600}.card .l{color:var(--muted);font-size:13px;text-transform:uppercase;letter-spacing:.04em}
table{width:100%;border-collapse:collapse;margin-top:12px}th,td{text-align:left;padding:9px 10px;border-bottom:1px solid var(--line);vertical-align:middle}
th{background:var(--bg);font-size:13px;color:var(--muted)}td.n{text-align:right;font-variant-numeric:tabular-nums;white-space:nowrap}
.chip{display:inline-block;padding:2px 10px;border-radius:999px;font-size:12px;font-weight:600;color:#fff}
.passed{background:var(--ok)}.failed{background:var(--fail)}.skipped,.missing{background:var(--skip)}
.bar{display:flex;height:10px;border-radius:5px;overflow:hidden;background:#eaeef2;min-width:140px}.bar span{display:block}
.meter{position:relative;height:10px;border-radius:5px;background:#eaeef2;min-width:120px}.meter span{position:absolute;inset:0 auto 0 0;border-radius:5px}
details{border:1px solid var(--line);border-radius:8px;margin:8px 0;padding:0 12px}summary{cursor:pointer;padding:10px 0;font-weight:600}
details table{margin:0 0 10px}.msg{color:var(--fail);font-size:12px;font-family:ui-monospace,monospace}
footer{color:var(--muted);font-size:12px;margin-top:40px}
"""


def meter(value):
    return f'<div class="meter"><span style="width:{value:.1f}%;background:var(--{"ok" if value >= 80 else "skip" if value >= 60 else "fail"})"></span></div>'


def stacked(counts):
    total = sum(counts.values()) or 1
    parts = "".join(
        f'<span class="{s}" style="width:{100 * counts[s] / total:.2f}%"></span>' for s in STATUSES if counts[s])
    return f'<div class="bar">{parts}</div>'


def render_html(version, generated, totals, features, modules, coverage):
    e = html.escape
    cards = [
        ("Version", e(version)),
        ("Tests", f'{totals["total"]}'),
        ("Passed", f'<span style="color:var(--ok)">{totals["passed"]}</span>'),
        ("Failed", f'<span style="color:var(--fail)">{totals["failed"]}</span>'),
        ("Skipped", f'<span style="color:var(--skip)">{totals["skipped"]}</span>'),
        ("Line coverage", f'{coverage["line"]:.1f}%'),
        ("Method coverage", f'{coverage["method"]:.1f}%'),
    ]
    out = [f"<!doctype html><html lang=en><head><meta charset=utf-8><meta name=viewport content='width=device-width,initial-scale=1'>"
           f"<title>criteria-filter {e(version)} test report</title><style>{CSS}</style></head><body>",
           f"<header><h1>criteria-filter {e(version)}</h1><p>Test and coverage report &middot; generated {e(generated)}</p></header><main>",
           '<div class="cards">' + "".join(f'<div class="card"><div class="l">{l}</div><div class="v">{v}</div></div>' for l, v in cards) + "</div>",
           "<h2>Functionality</h2><table><tr><th>Functionality</th><th>Status</th><th>Results</th><th class=n>Passed</th><th class=n>Failed</th><th class=n>Skipped</th><th class=n>Time</th></tr>"]
    for f in features:
        c = f["counts"]
        out.append(f'<tr><td>{e(f["name"])}</td><td><span class="chip {f["status"]}">{f["status"]}</span></td><td>{stacked(c)}</td>'
                   f'<td class=n>{c["passed"]}</td><td class=n>{c["failed"]}</td><td class=n>{c["skipped"]}</td><td class=n>{f["time"]:.2f}s</td></tr>')
    out.append("</table>")
    if modules:
        out.append("<h2>Coverage by module</h2><table><tr><th>Module</th><th>Line coverage</th><th class=n>Lines</th><th>Method coverage</th><th class=n>Methods</th><th class=n>Branches</th></tr>")
        for name, m in sorted(modules.items()):
            line, method, branch = (pct(*m[k]) for k in ("line", "method", "branch"))
            out.append(f'<tr><td>{e(name)}</td><td>{meter(line)}</td><td class=n>{line:.1f}% ({m["line"][0]}/{m["line"][1]})</td>'
                       f'<td>{meter(method)}</td><td class=n>{method:.1f}% ({m["method"][0]}/{m["method"][1]})</td>'
                       f'<td class=n>{"n/a" if not m["branch"][1] else f"{branch:.1f}%"}</td></tr>')
        out.append("</table>")
    out.append("<h2>Test cases</h2>")
    for f in features:
        if not f["cases"]:
            continue
        open_attr = " open" if f["status"] == "failed" else ""
        out.append(f'<details{open_attr}><summary><span class="chip {f["status"]}">{f["status"]}</span> {e(f["name"])} '
                   f'&middot; {sum(f["counts"].values())} tests</summary><table><tr><th>Class</th><th>Test</th><th>Status</th><th class=n>Time</th></tr>')
        for c in sorted(f["cases"], key=lambda c: (("failed", "skipped", "passed").index(c["status"]), c["class"], c["name"])):
            msg = f'<div class="msg">{e(c["message"])}</div>' if c["message"] else ""
            out.append(f'<tr><td>{e(c["class"])}</td><td>{e(c["name"])}{msg}</td><td><span class="chip {c["status"]}">{c["status"]}</span></td><td class=n>{c["time"]:.3f}s</td></tr>')
        out.append("</table></details>")
    out.append("<footer>Generated by .github/scripts/test_report.py from Surefire and JaCoCo reports.</footer></main></body></html>")
    return "\n".join(out)


def render_markdown(version, totals, features, coverage, report_url):
    icon = {"passed": "🟢", "failed": "🔴", "skipped": "🟡", "missing": "⚪"}
    lines = [f"## criteria-filter {version} — test report", "",
             f"**{totals['passed']}/{totals['total']} tests passed** · {totals['failed']} failed · {totals['skipped']} skipped · "
             f"line coverage **{coverage['line']:.1f}%** · method coverage **{coverage['method']:.1f}%**", "",
             "| Functionality | Status | Passed | Failed | Skipped |", "|---|---|--:|--:|--:|"]
    for f in features:
        c = f["counts"]
        lines.append(f"| {f['name']} | {icon[f['status']]} {f['status']} | {c['passed']} | {c['failed']} | {c['skipped']} |")
    failures = [c for f in features for c in f["cases"] if c["status"] == "failed"]
    if failures:
        lines += ["", "### Failures", ""] + [f"- `{c['class']}.{c['name']}`: {c['message']}" for c in failures]
    if report_url:
        lines += ["", f"Full visual report: {report_url}"]
    return "\n".join(lines) + "\n"


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--surefire", default="target/surefire-reports")
    parser.add_argument("--jacoco", default="target/site/jacoco/jacoco.csv")
    parser.add_argument("--features", default=os.path.join(os.path.dirname(__file__), "report-features.json"))
    parser.add_argument("--version", required=True)
    parser.add_argument("--out", default="target/test-report")
    parser.add_argument("--report-url", default="")
    args = parser.parse_args()

    config = json.load(open(args.features))
    cases = parse_surefire(args.surefire)
    if not cases:
        raise SystemExit(f"No Surefire reports found in {args.surefire}")
    features = group_by_feature(cases, config["features"])
    modules = parse_jacoco(args.jacoco, config.get("modules", {}))

    totals = {status: sum(1 for c in cases if c["status"] == status) for status in STATUSES}
    totals["total"] = len(cases)
    coverage = {}
    for kind in ("line", "method", "branch"):
        covered = sum(m[kind][0] for m in modules.values())
        total = sum(m[kind][1] for m in modules.values())
        coverage[kind] = pct(covered, total)

    generated = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    os.makedirs(os.path.join(args.out, "badges"), exist_ok=True)
    with open(os.path.join(args.out, "index.html"), "w") as handle:
        handle.write(render_html(args.version, generated, totals, features, modules, coverage))
    with open(os.path.join(args.out, "summary.md"), "w") as handle:
        handle.write(render_markdown(args.version, totals, features, coverage, args.report_url))

    tests_message = f"{totals['passed']} passed" + (f", {totals['failed']} failed" if totals["failed"] else "") \
        + (f", {totals['skipped']} skipped" if totals["skipped"] else "")
    badges = {
        "release": badge("release", f"v{args.version}" if not args.version.endswith("-SNAPSHOT") else args.version, "blue"),
        "tests": badge("tests", tests_message, "red" if totals["failed"] else ("yellow" if totals["skipped"] else "brightgreen")),
        "coverage": badge("coverage", f"{coverage['line']:.0f}%", coverage_color(coverage["line"])),
        "function-coverage": badge("function coverage", f"{coverage['method']:.0f}%", coverage_color(coverage["method"])),
    }
    for name, data in badges.items():
        with open(os.path.join(args.out, "badges", f"{name}.json"), "w") as handle:
            json.dump(data, handle)
            handle.write("\n")
    with open(os.path.join(args.out, "report.json"), "w") as handle:
        json.dump({"version": args.version, "generated": generated, "tests": totals, "coverage": coverage,
                   "features": [{k: f[k] for k in ("name", "status", "counts")} for f in features]}, handle, indent=2)
    print(f"{totals['passed']}/{totals['total']} passed, {totals['failed']} failed, {totals['skipped']} skipped; "
          f"line {coverage['line']:.1f}%, method {coverage['method']:.1f}% -> {args.out}")


if __name__ == "__main__":
    main()
