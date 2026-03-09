# P4a – Wave 3: Configuration (Haiku/Gemini)

**Phase:** P4a – Enterprise Features
**Agent:** Haiku / Gemini
**Complexity:** Easy
**Total Effort:** ~1 MD

---

## P4a-W3-001: Docker Compose – P4a Services

**Type:** Infrastructure
**Effort:** 0.5 MD

**Tasks:**
- [ ] Add MS-NOTIF, MS-VER, MS-AUDIT, MS-SRCH to docker-compose.yml
- [ ] Dapr sidecar configs
- [ ] Nginx routing updates
- [ ] SMTP test server (MailHog) for local dev

---

## P4a-W3-002: Email Templates

**Type:** Configuration
**Effort:** 0.5 MD

**Tasks:**
- [ ] HTML email templates (Thymeleaf):
  - Processing completed
  - Processing failed
  - Report rejected (with comment)
  - Deadline approaching
  - Deadline missed
- [ ] Store in `ms-notif/src/main/resources/templates/email/`
