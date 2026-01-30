# Gap Analysis & Status Report
**Date:** 2026-01-30
**Auditor:** Agtigravity (AI Agent)
**Reference:** `docs/tasks/status_20260130.md`

---

## 1. Executive Summary (Celkový stav)
**Status:** 🟡 **YELLOW**

Projekt má pevné základy (Backend Architecture, React Setup, MSAL Integration), ale obsahuje **kritické konfigurační chyby v autentizaci** a "placeholder" kód, který brání plnému nasazení. Kód strukturálně odpovídá standardům, ale implementační detaily (Scopes, Error Handling) vyžadují okamžitou opravu.

**Hlavní rizika:**
1.  **Auth Scope Mismatch:** Frontend žádá o token s jiným scopem (`client-id/user_impersonation`), než jaký pravděpodobně očekává API intercepter nebo Azure nastavení (`api://client-id/...`).
2.  **Placeholders:** `authConfig.ts` obsahuje defaultní hodnoty `"YOUR_CLIENT_ID"`, což může vést k tichému selhání konfigruace.
3.  **Missing Error Boundaries:** Chybějící globální ošetření chyb v UI.

---

## 2. Kontrola oproti Project Charter (Scope Check)

| Feature | Stav v kódu | Poznámka |
| --- | --- | --- |
| **Auth (Azure Entra ID)** | 🟡 Částečně | MSAL v3 implementován, `main.tsx` OK. **Chyba:** Nesoulad mezi `loginRequest` a `tokenRequest` scopes v `axios.ts`. |
| **Axios & API Comms** | 🟢 Hotovo | Centralizovaná instance v `axios.ts` s interceptorem pro Bearer token. |
| **Dashboard & Routing** | 🟡 Částečně | Routing funkční (`App.tsx`), ale hlavní stránky (Dashboard, Analytics, Admin) jsou jen **inline placeholder komponenty**. |
| **Práce se soubory** | 🟡 Částečně | Existují routy pro `ImportOpex` a `ExcelImport`, ale chybí klientská validace souborů (požadovaná k Charteru). |
| **DevOps** | 🟢 Hotovo | `docker-compose.yml` a `.env` struktura existuje. |

---

## 3. Technický Audit (Standards & Defaults Compliance)

*   **Architektura:** ✅ **Dodrženo.**
    *   Logika autentizace je správně oddělena do `msalInstance.ts`, `authConfig.ts` a `main.tsx`.
    *   API volání jsou centralizována v `axios.ts` (Interceptor pattern).
*   **Auth Implementation:** ❌ **Porušení.**
    *   V `frontend/src/api/axios.ts` se pro získání API tokenu používá `...loginRequest`. V `authConfig.ts` má `loginRequest` scope `${clientId}/user_impersonation`, zatímco `tokenRequest` má `api://${clientId}/user_impersonation`. Pokud Backend očekává `api://` prefix (což je standard), volání API selžou na 401/403.
*   **Code Quality / Styling:** ⚠️ **S výhradami.**
    *   `App.tsx` obsahuje inline definice komponent (`const Dashboard = ...`), což porušuje princip separace (Single Responsibility).
    *   Styling pomocí Tailwind CSS je konzistentní.
*   **Error Handling:** ❌ **Chybí.**
    *   Aplikace postrádá `ErrorBoundary` (požadováno v Charteru - To-Do). Pád komponenty shodí celou aplikaci (Bílá obrazovka).

---

## 4. Seznam Nálezech (Actionable Items)

### A. Kritické Chyby (Bugs)
*   **[frontend/src/api/axios.ts]**: Nesprávný Scope v `acquireTokenSilent`.
    *   *Problém:* Interceptor používá `loginRequest` (řádek 39), který nemusí obsahovat správný resource scope pro API (`api://...`).
    *   *Řešení:* Použít `tokenRequest` (importovat z authConfig) nebo sjednotit definice.
*   **[frontend/src/authConfig.ts]**: Nebezpečné default hodnoty.
    *   *Problém:* `const clientId = ... || "YOUR_CLIENT_ID"`.
    *   *Řešení:* Odstranit stringové literály fallbacků. Pokud `env` chybí, aplikace by měla failnout při startu (nebo vyhodit jasnou chybu), ne běžet s "YOUR_CLIENT_ID".

### B. Chybějící Implementace (Missing Features)
*   **[frontend/src/components/ErrorBoundary.tsx]**: Chybí komponenta pro zachytávání pádů Reactu (Status: Charter To-Do).
*   **[frontend/src/pages/*]**: Chybí reálné implementace pro Dashboard, Analytics a Admin (nyní jen placeholders v `App.tsx`).

### C. Technický Dluh & Refactoring
*   **[frontend/src/App.tsx]**: Refactor inline komponent.
    *   *Akce:* Přesunout `const Dashboard`, `Analytics` do samostatných souborů v `frontend/src/pages/`.
*   **[backend/app/core/config.py]**: `cors_origins_str` default obsahuje `http://localhost:5173`.
    *   *Poznámka:* Ujistit se, že to odpovídá `project_defaults.md` (tam je zmíněno 3000 pro React, ale Vite default je 5173). Toto je OK, pokud používáme Vite, ale aktualizovat Defaults dokumentaci by neškodilo.

---

## 5. Doporučený Next Step

1.  **Okamžitě opravit Auth Scopes:** Upravit `frontend/src/api/axios.ts` tak, aby používal správný `tokenRequest` s `api://` prefixem.
2.  **Vyčistit Placeholdery:** Odstranit inline komponenty z `App.tsx` a vytvořit základní soubory stránek.
3.  **Implementovat ErrorBoundary:** Přidat základní error barrier do `main.tsx` nebo `App.tsx`.
