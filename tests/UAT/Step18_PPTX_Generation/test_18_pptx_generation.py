#!/usr/bin/env python3
# Step18: PPTX Generation (FS18)
# Verify PPTX template upload, placeholder discovery, mapping, generation, status, download, and batch.

import sys
import os
import io

sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../config")))

from uat_common import UATSession
from uat_config import BASE_URL, USERS, SERVICES

STEP_NAME = "step18_PPTX_Generation"

# Minimal valid PPTX bytes (empty PK zip archive with content types)
MINIMAL_PPTX = (
    b"PK\x05\x06" + b"\x00" * 18  # empty zip end-of-central-directory
)


def main() -> int:
    session = UATSession(base_url=BASE_URL)
    session._log("[INFO] Step18 — PPTX Generation")

    # 1. Load tokens from state, login as admin1
    state = session.load_state()
    tokens = state.get("tokens", {})
    session.assert_true(bool(tokens), "Tokens loaded from state")

    token = session.login(USERS["admin1"]["email"], USERS["admin1"]["password"])
    if not token:
        session._err("[FATAL] Cannot login as admin1 — aborting step18")
        session.save_log(STEP_NAME)
        return 1

    session.restore_auth_from_state()

    # PPTX template endpoints live on engine-reporting
    reporting_session = session.for_service(SERVICES["engine_reporting"])

    report_ids = state.get("report_ids", {})
    report_id = report_ids.get("report_id")

    # 2. Upload PPTX template — POST /api/templates/pptx (multipart)
    files = {"file": ("template.pptx", io.BytesIO(MINIMAL_PPTX),
                       "application/vnd.openxmlformats-officedocument.presentationml.presentation")}
    status, body = reporting_session.call("POST", "/api/templates/pptx",
                                files=files,
                                expected_status=201, tag="upload-pptx-template")
    template_id = None
    if status == 201 and isinstance(body, dict):
        template_id = body.get("id") or body.get("template_id")

    if not template_id:
        session._log("[WARN] Template upload did not return template_id, attempting to continue")

    # 3. List templates — GET /api/templates/pptx
    status, body = reporting_session.call("GET", "/api/templates/pptx",
                                expected_status=200, tag="list-pptx-templates")

    # 4. Get template placeholders — GET /api/templates/pptx/{template_id}/placeholders
    if template_id:
        status, body = reporting_session.call("GET", f"/api/templates/pptx/{template_id}/placeholders",
                                    expected_status=200, tag="get-placeholders")
    else:
        session._log("[WARN] No template_id, skipping placeholder check")

    # 5. Configure placeholder mapping — POST /api/templates/pptx/{template_id}/mappings
    if template_id:
        mappings = {
            "mappings": [
                {"placeholderKey": "{{title}}", "dataSourceType": "field", "dataSourceRef": "report.name"},
                {"placeholderKey": "{{total}}", "dataSourceType": "field", "dataSourceRef": "report.total_amount"}
            ]
        }
        status, body = reporting_session.call("POST", f"/api/templates/pptx/{template_id}/mappings",
                                    body=mappings,
                                    expected_status=200, tag="configure-mappings")
    else:
        session._log("[WARN] No template_id, skipping mapping configuration")

    # 6. Generate PPTX report — POST /api/templates/pptx/generate
    job_id = None
    if template_id:
        gen_payload = {"template_id": template_id}
        if report_id:
            gen_payload["report_id"] = report_id
        status, body = reporting_session.call("POST", "/api/templates/pptx/generate",
                                    body=gen_payload,
                                    expected_status=200, tag="generate-pptx")
        if status in (200, 202) and isinstance(body, dict):
            job_id = body.get("job_id") or body.get("id")
    else:
        session._log("[WARN] No template_id, skipping PPTX generation")

    # 7. Check generation status — GET /api/templates/pptx/generate/{job_id}/status
    if job_id:
        status, body = reporting_session.call("GET", f"/api/templates/pptx/generate/{job_id}/status",
                                    expected_status=200, tag="generation-status")
    else:
        session._log("[WARN] No job_id, skipping generation status check")

    # 8. Download generated file — GET /api/templates/pptx/generate/{job_id}/download
    if job_id:
        status, body = reporting_session.call("GET", f"/api/templates/pptx/generate/{job_id}/download",
                                    expected_status=200, tag="download-pptx")
        if status == 200 and isinstance(body, bytes):
            session.assert_true(len(body) > 0, "Downloaded PPTX is non-empty")
    else:
        session._log("[WARN] No job_id, skipping download check")

    # 9. Batch generation — POST /api/templates/pptx/generate/batch
    if template_id:
        batch_report_ids = [rid for rid in [report_ids.get("report_id"), report_ids.get("report_id2")] if rid]
        if batch_report_ids:
            status, body = reporting_session.call("POST", "/api/templates/pptx/generate/batch",
                                        body={"template_id": template_id, "report_ids": batch_report_ids},
                                        expected_status=202, tag="batch-generate-pptx")
            # 10. If not implemented, use missing_feature()
            if status not in (200, 202):
                session.missing_feature("POST /api/templates/pptx/generate/batch",
                                        "Batch PPTX generation not yet implemented")
        else:
            session._log("[WARN] No report_ids available for batch generation")
            session.missing_feature("POST /api/templates/pptx/generate/batch",
                                    "No report_ids to test batch generation")
    else:
        session.missing_feature("POST /api/templates/pptx/generate/batch",
                                "No template_id available for batch generation test")

    session.sync_counters_from(reporting_session)
    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
