#!/usr/bin/env python3
# Web UAT Test Runner for ReportAutomatization
# Runs all frontend UI tests and generates UAT report

import sys
import os
import re
import subprocess
import datetime

TESTS_DIR = os.path.dirname(os.path.abspath(__file__))

if sys.stdout.encoding != 'utf-8':
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    if hasattr(sys.stderr, 'reconfigure'):
        sys.stderr.reconfigure(encoding='utf-8', errors='replace')

# Test modules: (path, name)
TEST_MODULES = [
    # Step07: Admin UI (FS07)
    ("tests/Step07_Admin_UI/test_admin_ui.py", "Step07 - Admin UI"),

    # Step09: Frontend SPA (FS09)
    ("tests/Step09_Auth_Navigation/test_auth_navigation.py", "Step09 - Auth & Navigation"),
    ("tests/Step09_File_Upload/test_file_upload.py", "Step09 - File Upload UI"),
    ("tests/Step09_Viewer/test_viewer.py", "Step09 - File Viewer"),

    # Step11: Dashboards & SQL Reporting (FS11)
    ("tests/Step11_Dashboards/test_dashboards.py", "Step11 - Dashboards"),

    # Step13: Notification Center (FS13)
    ("tests/Step13_Notifications/test_notifications.py", "Step13 - Notifications"),

    # Step14: Data Versioning & Diff Tool (FS14)
    ("tests/Step14_Versioning/test_versioning.py", "Step14 - Versioning"),

    # Step15: Schema Mapping Registry (FS15)
    ("tests/Step15_Schema_Mapping/test_schema_mapping.py", "Step15 - Schema Mapping"),

    # Step16: Audit & Compliance (FS16)
    ("tests/Step16_Audit/test_audit.py", "Step16 - Audit Log"),

    # Step17: Report Lifecycle (FS17)
    ("tests/Step17_Report_Lifecycle/test_report_lifecycle.py", "Step17 - Report Lifecycle"),

    # Step18: PPTX Generation (FS18)
    ("tests/Step18_PPTX_Generation/test_pptx_generation.py", "Step18 - PPTX Generation"),

    # Step19: Form Builder & Filling (FS19)
    ("tests/Step19_Form_Builder/test_form_builder.py", "Step19 - Form Builder"),
    ("tests/Step19_Form_Filling/test_form_filling.py", "Step19 - Form Filling"),

    # Step20: Period Management (FS20)
    ("tests/Step20_Period_Dashboard/test_period_dashboard.py", "Step20 - Period Dashboard"),
]


# ---------------------------------------------------------------------------
# Output parsing helpers
# ---------------------------------------------------------------------------
_RE_OK = re.compile(r"\[OK\]\s+(.+)")
_RE_FAIL = re.compile(r"\[FAIL\]\s+(.+)")
_RE_MISSING = re.compile(r"\[MISSING_FEATURE\]\s+(.+)")
_RE_PASS_FAIL_SUMMARY = re.compile(r"PASSED:\s*(\d+)\s+FAILED:\s*(\d+)")
_RE_TEST_NAME = re.compile(r"\[TEST\]\s+(.+)")


def parse_test_output(stdout: str, stderr: str) -> dict:
    """Parse test stdout/stderr into structured results."""
    combined = stdout + "\n" + stderr

    passed = _RE_OK.findall(combined)
    failed = _RE_FAIL.findall(combined)
    missing = _RE_MISSING.findall(combined)
    test_names = _RE_TEST_NAME.findall(combined)

    summary_match = _RE_PASS_FAIL_SUMMARY.search(combined)
    pass_count = int(summary_match.group(1)) if summary_match else len(passed)
    fail_count = int(summary_match.group(2)) if summary_match else len(failed)

    missing_features = []
    for m in missing:
        parts = m.split(" — ", 1)
        feature_name = parts[0].strip() if parts else m
        description = parts[1].strip() if len(parts) > 1 else ""
        missing_features.append({"feature": feature_name, "description": description})

    return {
        "passed": passed,
        "failed": failed,
        "missing_features": missing_features,
        "test_names": test_names,
        "pass_count": pass_count,
        "fail_count": fail_count,
    }


def run_test(test_path: str, test_name: str) -> dict:
    """Run a single test module and return structured result."""
    print(f"\n{'='*70}")
    print(f"Running: {test_name}")
    print(f"Path: {test_path}")
    print(f"{'='*70}")

    start_time = datetime.datetime.now()

    try:
        result = subprocess.run(
            [sys.executable, test_path],
            capture_output=True,
            text=True,
            timeout=300,
            cwd=TESTS_DIR
        )

        end_time = datetime.datetime.now()
        duration = (end_time - start_time).total_seconds()

        print(f"\n[RESULT] Exit code: {result.returncode}")
        print(f"[RESULT] Duration: {duration:.2f}s")

        if result.stdout:
            print(f"\n--- STDOUT ---")
            print(result.stdout[-2000:] if len(result.stdout) > 2000 else result.stdout)

        if result.stderr:
            print(f"\n--- STDERR ---")
            print(result.stderr[-1000:] if len(result.stderr) > 1000 else result.stderr)

        parsed = parse_test_output(result.stdout, result.stderr)

        if result.returncode == 0:
            status = "PASSED"
        elif result.returncode == -1:
            status = "TIMEOUT"
        else:
            status = "FAILED"

        return {
            "name": test_name,
            "path": test_path,
            "status": status,
            "exit_code": result.returncode,
            "duration": duration,
            "stdout": result.stdout,
            "stderr": result.stderr,
            **parsed,
        }

    except subprocess.TimeoutExpired:
        print(f"[ERROR] Test timed out after 300s")
        return {
            "name": test_name,
            "path": test_path,
            "status": "TIMEOUT",
            "exit_code": -1,
            "duration": 300.0,
            "stdout": "",
            "stderr": "Timeout after 300s",
            "passed": [], "failed": [], "missing_features": [],
            "test_names": [], "pass_count": 0, "fail_count": 0,
        }
    except Exception as e:
        print(f"[ERROR] Failed to run test: {e}")
        return {
            "name": test_name,
            "path": test_path,
            "status": "ERROR",
            "exit_code": -1,
            "duration": 0,
            "stdout": "",
            "stderr": str(e),
            "passed": [], "failed": [], "missing_features": [],
            "test_names": [], "pass_count": 0, "fail_count": 0,
        }


# ---------------------------------------------------------------------------
# Report generation
# ---------------------------------------------------------------------------
def generate_report(results: list, timestamp: str) -> str:
    """Generate structured Markdown UAT report."""
    total = len(results)
    passed_modules = sum(1 for r in results if r["status"] == "PASSED")
    failed_modules = sum(1 for r in results if r["status"] in ("FAILED", "ERROR"))
    timeout_modules = sum(1 for r in results if r["status"] == "TIMEOUT")
    skipped_modules = sum(1 for r in results if r["status"] == "SKIPPED")

    total_assertions_pass = sum(r.get("pass_count", 0) for r in results)
    total_assertions_fail = sum(r.get("fail_count", 0) for r in results)
    total_missing = sum(len(r.get("missing_features", [])) for r in results)
    total_duration = sum(r.get("duration", 0) for r in results)

    lines = []
    lines.append("# RA Web UI UAT Test Report")
    lines.append(f"\nGenerated: {timestamp}")
    lines.append(f"Base URL: `http://localhost:5173` (Vite dev server)")
    lines.append(f"Total duration: {total_duration:.1f}s")
    lines.append("")

    # Summary Table
    lines.append("## Summary")
    lines.append("")
    lines.append("| Metric | Count |")
    lines.append("|--------|-------|")
    lines.append(f"| Test Modules | {total} |")
    lines.append(f"| Modules PASSED | {passed_modules} |")
    lines.append(f"| Modules FAILED | {failed_modules} |")
    lines.append(f"| Modules TIMEOUT | {timeout_modules} |")
    lines.append(f"| Modules SKIPPED | {skipped_modules} |")
    lines.append(f"| Total Assertions Passed | {total_assertions_pass} |")
    lines.append(f"| Total Assertions Failed | {total_assertions_fail} |")
    lines.append(f"| Missing Features | {total_missing} |")
    lines.append("")

    # Module Results Table
    lines.append("## Module Results")
    lines.append("")
    lines.append("| # | Module | Status | Pass | Fail | Missing | Duration |")
    lines.append("|---|--------|--------|------|------|---------|----------|")
    for i, r in enumerate(results, 1):
        status_icon = {"PASSED": "PASS", "FAILED": "FAIL", "TIMEOUT": "TIME", "SKIPPED": "SKIP", "ERROR": "ERR"}.get(r["status"], "?")
        lines.append(
            f"| {i} | {r['name']} | {status_icon} | "
            f"{r.get('pass_count', 0)} | {r.get('fail_count', 0)} | "
            f"{len(r.get('missing_features', []))} | {r.get('duration', 0):.1f}s |"
        )
    lines.append("")

    # Failed Tests Detail
    failed_results = [r for r in results if r["status"] in ("FAILED", "ERROR", "TIMEOUT")]
    if failed_results:
        lines.append("## Failed Modules Detail")
        lines.append("")
        for r in failed_results:
            lines.append(f"### {r['name']} ({r['status']})")
            lines.append(f"- **Path:** `{r['path']}`")
            lines.append(f"- **Exit code:** {r['exit_code']}")
            lines.append(f"- **Duration:** {r.get('duration', 0):.1f}s")
            lines.append(f"- **Assertions:** {r.get('pass_count', 0)} passed, {r.get('fail_count', 0)} failed")
            lines.append("")

            if r.get("failed"):
                lines.append("**Failed assertions:**")
                for f in r["failed"]:
                    lines.append(f"- `{f}`")
                lines.append("")

    # Missing Features
    all_missing = []
    for r in results:
        for mf in r.get("missing_features", []):
            all_missing.append({"module": r["name"], **mf})

    if all_missing:
        lines.append("## Missing Features")
        lines.append("")
        lines.append("| # | Module | Feature | Description |")
        lines.append("|---|--------|---------|-------------|")
        for i, mf in enumerate(all_missing, 1):
            lines.append(f"| {i} | {mf['module']} | `{mf['feature']}` | {mf['description']} |")
        lines.append("")

    # Passed Modules
    passed_results = [r for r in results if r["status"] == "PASSED"]
    if passed_results:
        lines.append("## Passed Modules")
        lines.append("")
        for r in passed_results:
            lines.append(f"- **{r['name']}**: {r.get('pass_count', 0)} assertions passed ({r.get('duration', 0):.1f}s)")
        lines.append("")

    return "\n".join(lines)


def main():
    print("""
    ====================================================================
       ReportAutomatization - Web UI UAT Test Suite
    ====================================================================
    """)

    timestamp_str = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
    timestamp_iso = datetime.datetime.now().isoformat(timespec="seconds")
    log_dir = os.path.normpath(os.path.join(TESTS_DIR, "logs"))
    os.makedirs(log_dir, exist_ok=True)

    log_file = os.path.join(log_dir, f"web_uat_run_{timestamp_str}.log")
    report_file = os.path.join(log_dir, f"web_uat_report_{timestamp_str}.md")

    results = []
    total_passed = 0
    total_failed = 0
    total_skipped = 0

    for test_rel_path, test_name in TEST_MODULES:
        test_path = os.path.normpath(os.path.join(TESTS_DIR, test_rel_path))

        if not os.path.exists(test_path):
            print(f"[WARN] Test not found: {test_path}")
            total_skipped += 1
            results.append({
                "name": test_name,
                "path": test_rel_path,
                "status": "SKIPPED",
                "reason": "Not found",
                "exit_code": -1,
                "duration": 0,
                "passed": [], "failed": [], "missing_features": [],
                "test_names": [], "pass_count": 0, "fail_count": 0,
            })
            continue

        result = run_test(test_path, test_name)
        results.append(result)

        if result["status"] == "PASSED":
            total_passed += 1
        else:
            total_failed += 1

    # Console summary
    print(f"""
    ====================================================================
                    Web UI UAT Test Suite Summary
    ====================================================================
      Total Modules: {len(results)}
      Passed:        {total_passed}
      Failed:        {total_failed}
      Skipped:       {total_skipped}
    ====================================================================
    """)

    print("\nDetailed Results:")
    print("-" * 80)
    print(f"{'Module':<45} {'Status':<8} {'Pass':>5} {'Fail':>5} {'Missing':>8} {'Time':>7}")
    print("-" * 80)

    for r in results:
        status_sym = {"PASSED": "PASS", "FAILED": "FAIL", "TIMEOUT": "TIME", "SKIPPED": "SKIP", "ERROR": "ERR"}.get(r["status"], "?")
        print(
            f"{r['name']:<45} {status_sym:<8} "
            f"{r.get('pass_count', 0):>5} {r.get('fail_count', 0):>5} "
            f"{len(r.get('missing_features', [])):>8} "
            f"{r.get('duration', 0):>6.1f}s"
        )

    print("-" * 80)

    # Write log file
    with open(log_file, "w", encoding="utf-8") as f:
        f.write(f"Web UI UAT Test Run - {timestamp_str}\n")
        f.write("=" * 70 + "\n\n")
        for r in results:
            f.write(f"{r['status']}: {r['name']}\n")
            f.write(f"  Path: {r['path']}\n")
            if 'exit_code' in r:
                f.write(f"  Exit Code: {r['exit_code']}\n")
            if 'reason' in r:
                f.write(f"  Reason: {r['reason']}\n")
            f.write(f"  Assertions: {r.get('pass_count', 0)} passed, {r.get('fail_count', 0)} failed\n")
            f.write(f"  Missing Features: {len(r.get('missing_features', []))}\n")
            if r.get('failed'):
                for fail_msg in r['failed']:
                    f.write(f"  [FAIL] {fail_msg}\n")
            if r.get('missing_features'):
                for mf in r['missing_features']:
                    f.write(f"  [MISSING] {mf['feature']}: {mf['description']}\n")
            f.write("\n")
        f.write(f"\nSummary: {total_passed} passed, {total_failed} failed, {total_skipped} skipped\n")

    print(f"\nLog file: {log_file}")

    # Generate UAT report
    report_md = generate_report(results, timestamp_iso)
    with open(report_file, "w", encoding="utf-8") as f:
        f.write(report_md)

    print(f"UAT Report: {report_file}")

    return 0 if total_failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
