# TASK: Step00 — Inicializace UAT prostředí

## Cíl

Ověřit, že platforma běží (health check), ověřit dev bypass autentizaci
a vygenerovat API klíče pro testovací organizace.

## Prerekvizity

- Nginx API gateway běží na portu 80
- Backend služby běží s `AUTH_MODE=development` (dev bypass)
- Organizace test-org-1 a test-org-2 existují (vytvořeny přes infra/init/setup.py)

## Kroky

1. **Health check**: `GET /health` → 200 OK (nginx)
2. **Dev bypass verify**: `GET /api/auth/verify` bez tokenu → 200 (HOLDING_ADMIN v dev mode)
3. **List organizací**: `GET /api/auth/admin/organizations` → zjistit org IDs
4. **Generování API klíčů**: Pro každou test roli:
   - `POST /api/auth/admin/api-keys` s `{name, role, organizationId}`
   - admin1 (ADMIN, test-org-1)
   - user1 (EDITOR, test-org-1)
   - admin2 (ADMIN, test-org-2)
   - user2 (VIEWER, test-org-2)
5. **Ověření API key auth**: `GET /api/auth/verify` s `X-API-Key` header → 200

## Očekávané výsledky

- Health endpoint vrátí 200
- Dev bypass auth verify vrátí 200
- 4 API klíče vygenerovány (raw key vrácen v response)
- API key auth funguje (verify vrátí 200 s X-API-Key)
- API klíče uloženy do `logs/uat_state.json` pro další kroky

## Poznámky

- Systém nepoužívá password-based login — autentizace je přes Azure AD (produkce) nebo API klíče (programový přístup)
- V dev mode (`AUTH_MODE=development`) je auth bypass — všechny requesty projdou jako HOLDING_ADMIN
- API klíče jsou scoped na organizaci + roli → simulují různé uživatele v testech
