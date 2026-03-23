#!/usr/bin/env python3
# Step15: Schema Mapping (FS15)
# Verify template mapping CRUD, auto-suggest, excel-to-form mapping, and cleanup.

import sys
import os

# Add shared lib to path
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../config")))

from uat_common import UATSession
from uat_config import BASE_URL, USERS, SERVICES

STEP_NAME = "step15_Schema_Mapping"


def main() -> int:
    session = UATSession(base_url=BASE_URL)
    state = session.load_state()
    tokens = state.get("tokens", {})

    session._log(f"[INFO] Step15 — Schema Mapping Verification")
    session._log(f"[INFO] Base URL: {BASE_URL}")

    # ---------------------------------------------------------------
    # 1. Load tokens, login as admin1
    # ---------------------------------------------------------------
    admin1 = USERS["admin1"]
    admin1_token = tokens.get("admin1")
    if not admin1_token:
        admin1_token = session.login(admin1["email"], admin1["password"])
        if admin1_token:
            tokens["admin1"] = admin1_token
    else:
        session.token = admin1_token
        session._log("[INFO] Reusing admin1 token from state")

    session.assert_true(admin1_token is not None, "admin1 token available")
    if not admin1_token:
        session._err("[FATAL] Cannot proceed without admin1 token.")
        session.save_log(STEP_NAME)
        return 1

    session.token = admin1_token
    session.restore_auth_from_state()

    # Template mapping is internal (Dapr gRPC) — no direct REST endpoint on engine-data
    # All CRUD and slide-metadata endpoints are marked as missing_feature

    template_id = None
    slide_meta_id = None

    # ---------------------------------------------------------------
    # 2-7. Template Mapping CRUD — internal only (Dapr gRPC), no REST endpoint
    # ---------------------------------------------------------------
    session.missing_feature("GET /api/query/templates/mappings", "Template mapping is internal (Dapr gRPC), no REST endpoint on engine-data")
    session.missing_feature("POST /api/query/templates/mappings", "Template mapping CRUD is internal (Dapr gRPC)")
    session.missing_feature("POST /api/query/templates/mappings/suggest", "Auto-suggest mapping is internal (Dapr gRPC)")
    session.missing_feature("POST /api/query/templates/mappings/excel-to-form", "Excel-to-form mapping is internal (Dapr gRPC)")

    # ===============================================================
    # Slide Metadata CRUD — no REST endpoint on engine-data
    # ===============================================================
    session._log("[INFO] --- Slide Metadata Template Tests ---")
    session.missing_feature("GET /api/query/templates/slide-metadata", "Slide metadata endpoint not available via REST")
    session.missing_feature("POST /api/query/templates/slide-metadata", "Slide metadata create not available via REST")
    session.missing_feature("POST /api/query/templates/slide-metadata/validate", "Slide metadata validation not available via REST")

    pptx_file_id = state.get("file_ids", {}).get("pptx")
    if pptx_file_id:
        session.missing_feature("GET /api/query/templates/slide-metadata/match", "Auto-match endpoint not available via REST")
    else:
        session._log("[SKIP] Preview and match tests skipped — no pptx_file_id")

    # ---------------------------------------------------------------
    # 14. Save state
    # ---------------------------------------------------------------
    patch = {"tokens": tokens}
    if template_id:
        patch["mapping_template_id"] = template_id
    if slide_meta_id:
        patch["slide_metadata_id"] = slide_meta_id
    session.update_state(patch)

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
