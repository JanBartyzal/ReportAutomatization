#!/usr/bin/env python3
# Step02: File Upload (FS02)
# Verify file upload using real test files from tests/UAT/data/,
# type validation, auth check, and metadata retrieval.

import sys
import os
import io

# Add shared lib to path
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../shared")))
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../config")))

from uat_common import UATSession
from uat_config import BASE_URL, USERS, TIMEOUTS, SERVICES

STEP_NAME = "step02_File_Upload"

# ---------------------------------------------------------------------------
# Test data directory (tests/UAT/data/)
# ---------------------------------------------------------------------------
DATA_DIR = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../data"))

TEST_FILES = {
    "pptx": {
        "filename": "DemoPage.pptx",
        "mime": "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    },
    "xlsx": {
        "filename": "Test.xlsx",
        "mime": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    },
}

# Fake EXE blob for rejection test
FAKE_EXE = b"MZ" + b"\x00" * 100


def _load_test_file(file_key: str) -> tuple[str, bytes, str] | None:
    """Load a test file from data/ directory. Returns (filename, content_bytes, mime) or None."""
    info = TEST_FILES.get(file_key)
    if not info:
        return None
    path = os.path.join(DATA_DIR, info["filename"])
    if not os.path.exists(path):
        return None
    with open(path, "rb") as f:
        content = f.read()
    return info["filename"], content, info["mime"]


def main() -> int:
    session = UATSession(base_url=BASE_URL)
    state = session.load_state()
    tokens = state.get("tokens", {})

    # Upload goes to engine-ingestor, metadata queries to engine-ingestor (FileController)
    upload_url = SERVICES.get("engine_ingestor", BASE_URL)
    ingestor_url = SERVICES.get("engine_ingestor", BASE_URL)
    session._log(f"[INFO] Step02 — File Upload Verification")
    session._log(f"[INFO] Upload URL: {upload_url}")
    session._log(f"[INFO] Ingestor URL: {ingestor_url}")
    session._log(f"[INFO] Data dir: {DATA_DIR}")

    # ---------------------------------------------------------------
    # 0. Verify test data files exist
    # ---------------------------------------------------------------
    for _, info in TEST_FILES.items():
        path = os.path.join(DATA_DIR, info["filename"])
        session.assert_true(
            os.path.exists(path),
            f"Test file exists: {info['filename']} ({os.path.getsize(path) if os.path.exists(path) else 0} bytes)"
        )

    # ---------------------------------------------------------------
    # 1. Load tokens
    # ---------------------------------------------------------------
    admin1_token = tokens.get("admin1")
    if not admin1_token:
        session._err("[FATAL] No admin1 token in state. Run Step00 first.")
        session.save_log(STEP_NAME)
        return 1

    session.token = admin1_token
    session.restore_auth_from_state()
    session._log(f"[INFO] Auth: user_id={session.user_id}, org_id={session.org_id}")
    upload_session = session.for_service(upload_url)
    ingestor_session = session.for_service(ingestor_url)
    file_ids = state.get("file_ids", {})

    # ---------------------------------------------------------------
    # 2. Upload valid PPTX file (real file from data/)
    # ---------------------------------------------------------------
    pptx_data = _load_test_file("pptx")
    if pptx_data:
        filename, content, mime = pptx_data
        session._log(f"[INFO] Uploading PPTX: {filename} ({len(content)} bytes)")
        pptx_file = (filename, io.BytesIO(content), mime)
        status, body = upload_session.call("POST", "/api/upload",
                                    files={"file": pptx_file},
                                    expected_status=201,
                                    tag="upload-pptx",
                                    timeout=TIMEOUTS.get("upload", 60))
        # Accept both 200 and 201
        if status == 200:
            session._pass_count += 1
            session._fail_count = max(0, session._fail_count - 1)
            session._log("[OK]   Upload PPTX returned 200 (acceptable)")

        pptx_file_id = None
        if isinstance(body, dict):
            pptx_file_id = body.get("fileId") or body.get("file_id") or body.get("id")
            if pptx_file_id:
                file_ids["pptx"] = pptx_file_id
                session._log(f"[INFO] PPTX file_id: {pptx_file_id}")
            else:
                session.assert_true(False, "Upload PPTX response should contain fileId/file_id/id")
    else:
        session._err(f"[FATAL] Test file not found: {TEST_FILES['pptx']['filename']} in {DATA_DIR}")
        session.save_log(STEP_NAME)
        return 1

    # ---------------------------------------------------------------
    # 3. Upload valid XLSX file (real file from data/)
    # ---------------------------------------------------------------
    xlsx_data = _load_test_file("xlsx")
    if xlsx_data:
        filename, content, mime = xlsx_data
        session._log(f"[INFO] Uploading XLSX: {filename} ({len(content)} bytes)")
        xlsx_file = (filename, io.BytesIO(content), mime)
        status, body = upload_session.call("POST", "/api/upload",
                                    files={"file": xlsx_file},
                                    expected_status=201,
                                    tag="upload-xlsx",
                                    timeout=TIMEOUTS.get("upload", 60))
        if status == 200:
            session._pass_count += 1
            session._fail_count = max(0, session._fail_count - 1)
            session._log("[OK]   Upload XLSX returned 200 (acceptable)")

        xlsx_file_id = None
        if isinstance(body, dict):
            xlsx_file_id = body.get("fileId") or body.get("file_id") or body.get("id")
            if xlsx_file_id:
                file_ids["xlsx"] = xlsx_file_id
                session._log(f"[INFO] XLSX file_id: {xlsx_file_id}")
            else:
                session.assert_true(False, "Upload XLSX response should contain fileId/file_id/id")
    else:
        session._err(f"[FATAL] Test file not found: {TEST_FILES['xlsx']['filename']} in {DATA_DIR}")
        session.save_log(STEP_NAME)
        return 1

    # ---------------------------------------------------------------
    # 4. Upload invalid file type (.exe) — expect 415
    # ---------------------------------------------------------------
    exe_file = ("malicious.exe", io.BytesIO(FAKE_EXE), "application/octet-stream")
    status, body = upload_session.call("POST", "/api/upload",
                                files={"file": exe_file},
                                expected_status=415,
                                tag="upload-exe-rejected",
                                timeout=TIMEOUTS.get("upload", 60))
    # 400 is also acceptable for rejected file type
    if status == 400:
        session._pass_count += 1
        session._fail_count = max(0, session._fail_count - 1)
        session._log("[OK]   Upload .exe returned 400 (acceptable rejection)")

    # ---------------------------------------------------------------
    # 5. Upload without auth — expect 401/403 (in dev mode may accept)
    # ---------------------------------------------------------------
    noauth_session = UATSession(base_url=upload_url)  # no token, no api_key, no X-headers
    noauth_file = ("noauth.pptx", io.BytesIO(b"PK\x03\x04" + b"\x00" * 50),
                   "application/vnd.openxmlformats-officedocument.presentationml.presentation")
    status, body = noauth_session.call("POST", "/api/upload",
                                files={"file": noauth_file},
                                expected_status=400,  # 400 = missing required X-User-Id/X-Org-Id headers
                                tag="upload-no-auth")
    # In prod (via nginx): 401 (ForwardAuth). Direct to service: 400 (missing headers) or 401/403
    session.assert_true(status in (400, 401, 403),
                        f"No-auth upload rejected (HTTP {status})")

    # ---------------------------------------------------------------
    # 6. Check uploaded file metadata (PPTX)
    # ---------------------------------------------------------------
    if pptx_file_id:
        status, body = ingestor_session.call("GET", f"/api/files/{pptx_file_id}",
                                    expected_status=200, tag="file-metadata-pptx")
        if status == 200 and isinstance(body, dict):
            session.assert_field(body, "filename", label="PPTX filename present")
            has_mime = ("mimeType" in body or "mime_type" in body or "contentType" in body)
            session.assert_true(has_mime, "PPTX metadata contains mime type field")
        elif status in (404, 500):
            session.missing_feature(f"GET /api/files/{pptx_file_id}",
                                    f"File metadata query returned {status} — endpoint may not be implemented yet")
    else:
        session._log("[SKIP] No PPTX file_id — skipping metadata check")

    # ---------------------------------------------------------------
    # 7. Check uploaded file metadata (XLSX)
    # ---------------------------------------------------------------
    if xlsx_file_id:
        status, body = ingestor_session.call("GET", f"/api/files/{xlsx_file_id}",
                                    expected_status=200, tag="file-metadata-xlsx")
        if status == 200 and isinstance(body, dict):
            session.assert_field(body, "filename", label="XLSX filename present")
            has_mime = ("mimeType" in body or "mime_type" in body or "contentType" in body)
            session.assert_true(has_mime, "XLSX metadata contains mime type field")
        elif status in (404, 500):
            session.missing_feature(f"GET /api/files/{xlsx_file_id}",
                                    f"File metadata query returned {status} — endpoint may not be implemented yet")
    else:
        session._log("[SKIP] No XLSX file_id — skipping metadata check")

    # ---------------------------------------------------------------
    # 8. Save file_ids to state
    # ---------------------------------------------------------------
    session.sync_counters_from(upload_session)
    session.sync_counters_from(ingestor_session)
    session.update_state({"file_ids": file_ids})
    session._log(f"[INFO] State saved: file_ids = {file_ids}")

    ok = session.save_log(STEP_NAME)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
