#!/usr/bin/env python3
"""
RA Platform Init Script
=============================
Nahrazuje unit-init C# projekt. Nastavuje platformu přes REST API volání
místo přímého zápisu do DB — nezávislé na interní DB struktuře.

Použití:
    python setup.py                          # výchozí config.json, localhost:5100
    python setup.py --config my-config.json  # vlastní config
    python setup.py --api-url https://...    # vlastní API URL
    python setup.py --wait 120              # čekat na API max 120s
    python setup.py --skip-step users        # přeskočit krok
    python setup.py --only-step superadmin   # spustit jen jeden krok
"""

import argparse
import json
import os
import sys
import time
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional

import requests

# ── Barvy pro výstup ─────────────────────────────────────────────────────────

class C:
    OK = "\033[92m"
    FAIL = "\033[91m"
    WARN = "\033[93m"
    INFO = "\033[96m"
    DIM = "\033[90m"
    BOLD = "\033[1m"
    END = "\033[0m"

def log_step(order: int, name: str):
    print(f"\n{C.BOLD}[{order:>2}] {name}{C.END}")
    print("─" * 60)

def log_ok(msg: str):
    print(f"  {C.OK}OK{C.END}  {msg}")

def log_fail(msg: str):
    print(f"  {C.FAIL}FAIL{C.END}  {msg}")

def log_skip(msg: str):
    print(f"  {C.DIM}SKIP{C.END}  {msg}")

def log_info(msg: str):
    print(f"  {C.INFO}INFO{C.END}  {msg}")

def log_warn(msg: str):
    print(f"  {C.WARN}WARN{C.END}  {msg}")


# ── API klient ───────────────────────────────────────────────────────────────

class ApiClient:
    """HTTP klient pro RA REST API."""

    def __init__(self, base_url: str):
        self.base_url = base_url.rstrip("/")
        self.session = requests.Session()
        self.session.headers.update({
            "Content-Type": "application/json",
            "Accept": "application/json",
        })
        self.token: Optional[str] = None

    def set_token(self, token: str):
        self.token = token
        self.session.headers["Authorization"] = f"Bearer {token}"

    def clear_token(self):
        self.token = None
        self.session.headers.pop("Authorization", None)

    def url(self, path: str) -> str:
        return f"{self.base_url}{path}"

    def get(self, path: str, **kwargs) -> requests.Response:
        return self.session.get(self.url(path), **kwargs)

    def post(self, path: str, data: dict | None = None, **kwargs) -> requests.Response:
        return self.session.post(self.url(path), json=data, **kwargs)

    def put(self, path: str, data: dict | None = None, **kwargs) -> requests.Response:
        return self.session.put(self.url(path), json=data, **kwargs)

    def delete(self, path: str, **kwargs) -> requests.Response:
        return self.session.delete(self.url(path), **kwargs)


# ── Konfigurace ──────────────────────────────────────────────────────────────

@dataclass
class AdminConfig:
    email: str
    password: str
    first_name: str = "Platform"
    last_name: str = "Administrator"
    tenant_id: str = "reportautomatization-platform"
    identity_provider: str = "local"
    azure_object_id: str = ""

@dataclass
class AzureAccountConfig:
    email: str
    azure_object_id: str
    azure_tenant_id: str = ""
    display_name: str = ""
    first_name: str = ""
    last_name: str = ""
    role: str = "viewer"
    organization_name: str = ""
    organization_slug: str = ""

@dataclass
class IdentityProviderConfig:
    provider_name: str
    provider_type: str = "oidc"
    client_id: str = ""
    issuer_url: str = ""
    redirect_uri: str = ""
    scopes: str = "openid profile email"
    organization_slug: str = ""

@dataclass
class InvoiceLineItemConfig:
    description: str = ""
    quantity: float = 1
    unit_price: float = 0
    period: str = ""

@dataclass
class InvoiceSeedConfig:
    organization_slug: str = ""
    invoice_number: str = ""
    status: str = "draft"
    currency: str = "USD"
    period_start: str = ""
    period_end: str = ""
    due_date: str = ""
    paid_at: str = ""
    tax_rate: float = 0
    tax_type: str = "standard"
    seller_country: str = "CZ"
    buyer_country: str = ""
    buyer_vat_id: str = ""
    billing_channel: str = "Card"
    line_items: list[InvoiceLineItemConfig] = field(default_factory=list)

@dataclass
class BillingSeedConfig:
    enabled: bool = False
    invoices: list[InvoiceSeedConfig] = field(default_factory=list)

@dataclass
class SlideMetadataConfig:
    """Konfigurace pro seed slide metadata šablon (engine-data:template)."""
    directory: str = ""
    enabled: bool = False

@dataclass
class DemoSeedConfig:
    data_directory: str = "./DemoData"
    enabled: bool = False

@dataclass
class InitConfig:
    admin: AdminConfig = field(default_factory=lambda: AdminConfig("admin@reportautomatization.com", ""))
    azure_accounts: list[AzureAccountConfig] = field(default_factory=list)
    identity_providers: list[IdentityProviderConfig] = field(default_factory=list)
    slide_metadata: SlideMetadataConfig = field(default_factory=SlideMetadataConfig)
    demo_seed: DemoSeedConfig = field(default_factory=DemoSeedConfig)
    billing_seed: BillingSeedConfig = field(default_factory=BillingSeedConfig)


def load_config(path: str) -> InitConfig:
    """Načte konfiguraci z JSON souboru."""
    with open(path, "r", encoding="utf-8") as f:
        raw = json.load(f)

    admin_raw = raw.get("Admin", {})
    admin = AdminConfig(
        email=admin_raw.get("Email", "admin@reportautomatization.com"),
        password=admin_raw.get("Password", "password"),
        first_name=admin_raw.get("FirstName", "Platform"),
        last_name=admin_raw.get("LastName", "Administrator"),
        tenant_id=admin_raw.get("TenantId", "reportautomatization-platform"),
        identity_provider=admin_raw.get("IdentityProvider", "local"),
        azure_object_id=admin_raw.get("AzureObjectId", ""),
    )

    azure_accounts = []
    for a in raw.get("AzureAccounts", []):
        azure_accounts.append(AzureAccountConfig(
            email=a["Email"],
            azure_object_id=a["AzureObjectId"],
            azure_tenant_id=a.get("AzureTenantId", ""),
            display_name=a.get("DisplayName", ""),
            first_name=a.get("FirstName", ""),
            last_name=a.get("LastName", ""),
            role=a.get("Role", "viewer"),
            organization_name=a.get("OrganizationName", ""),
            organization_slug=a.get("OrganizationSlug", ""),
        ))

    identity_providers = []
    for p in raw.get("IdentityProviders", []):
        # Expandovat env proměnné v hodnotách
        client_id = _expand_env(p.get("ClientId", ""))
        issuer_url = _expand_env(p.get("IssuerUrl", ""))
        redirect_uri = _expand_env(p.get("RedirectUri", ""))

        identity_providers.append(IdentityProviderConfig(
            provider_name=p["ProviderName"],
            provider_type=p.get("ProviderType", "oidc"),
            client_id=client_id,
            issuer_url=issuer_url,
            redirect_uri=redirect_uri,
            scopes=p.get("Scopes", "openid profile email"),
            organization_slug=p.get("OrganizationSlug", ""),
        ))

    slide_meta_raw = raw.get("SlideMetadata", {})
    slide_metadata = SlideMetadataConfig(
        directory=slide_meta_raw.get("Directory", ""),
        enabled=slide_meta_raw.get("Enabled", False),
    )

    demo_raw = raw.get("DemoSeed", {})
    demo_seed = DemoSeedConfig(
        data_directory=demo_raw.get("DataDirectory", "./DemoData"),
        enabled=demo_raw.get("Enabled", False),
    )

    billing_raw = raw.get("BillingSeed", {})
    billing_invoices = []
    for inv in billing_raw.get("Invoices", []):
        line_items = [
            InvoiceLineItemConfig(
                description=li.get("Description", ""),
                quantity=li.get("Quantity", 1),
                unit_price=li.get("UnitPrice", 0),
                period=li.get("Period", ""),
            )
            for li in inv.get("LineItems", [])
        ]
        billing_invoices.append(InvoiceSeedConfig(
            organization_slug=inv.get("OrganizationSlug", ""),
            invoice_number=inv.get("InvoiceNumber", ""),
            status=inv.get("Status", "draft"),
            currency=inv.get("Currency", "USD"),
            period_start=inv.get("PeriodStart", ""),
            period_end=inv.get("PeriodEnd", ""),
            due_date=inv.get("DueDate", ""),
            paid_at=inv.get("PaidAt", ""),
            tax_rate=inv.get("TaxRate", 0),
            tax_type=inv.get("TaxType", "standard"),
            seller_country=inv.get("SellerCountry", "CZ"),
            buyer_country=inv.get("BuyerCountry", ""),
            buyer_vat_id=inv.get("BuyerVatId", ""),
            billing_channel=inv.get("BillingChannel", "Card"),
            line_items=line_items,
        ))
    billing_seed = BillingSeedConfig(
        enabled=billing_raw.get("Enabled", False),
        invoices=billing_invoices,
    )

    return InitConfig(
        admin=admin,
        azure_accounts=azure_accounts,
        identity_providers=identity_providers,
        slide_metadata=slide_metadata,
        demo_seed=demo_seed,
        billing_seed=billing_seed,
    )


def _expand_env(value: str) -> str:
    """Nahradí ${VAR_NAME} hodnotami z env proměnných."""
    import re
    def replacer(m):
        var_name = m.group(1)
        return os.environ.get(var_name, m.group(0))
    return re.sub(r"\$\{(\w+)}", replacer, value)


# ── Init kroky ───────────────────────────────────────────────────────────────

class InitRunner:
    """Spouští jednotlivé init kroky přes REST API."""

    def __init__(self, api: ApiClient, config: InitConfig, config_dir: str = ""):
        self.api = api
        self.config = config
        self._config_dir = config_dir
        self.admin_token: Optional[str] = None
        # org slug → org id (z API responses)
        self.org_ids: dict[str, str] = {}
        self.results: list[tuple[str, bool, str]] = []

    def run_all(self, skip_steps: set[str] | None = None, only_step: str | None = None):
        steps = [
            ("health",    "Health Check",                self.step_health_check),
            ("superadmin", "Verify Admin Access",         self.step_register_superadmin),
            ("orgs",      "Load Organizations",           self.step_create_organizations),
            ("metadata",  "Seed Slide Metadata",          self.step_seed_slide_metadata),
            ("demo",      "Upload Demo Plans",            self.step_upload_demo_plans),
            ("billing",   "Seed Billing Invoices",        self.step_seed_billing),
        ]

        skip_steps = skip_steps or set()

        print(f"\n{C.BOLD}{'=' * 60}{C.END}")
        print(f"{C.BOLD}  RA Platform Init (REST API){C.END}")
        print(f"{C.BOLD}{'=' * 60}{C.END}")
        print(f"  API: {self.api.base_url}")

        # Pokud --only-step přeskočí superadmin, ověříme dev bypass tiše
        if only_step and only_step not in ("superadmin", "health") and not self.admin_token:
            try:
                self.api.session.headers["Authorization"] = "Bearer dev-bypass-token"
                r = self.api.get("/api/auth/verify")
                if r.status_code == 200:
                    self.admin_token = "dev-bypass"
            except Exception:
                pass

        # Načíst org_ids pokud je potřeba a ještě nejsou naplněné
        if only_step and only_step not in ("superadmin", "health", "orgs") and not self.org_ids and self.admin_token:
            # Spustit create_organizations tiše — je idempotentní (409 při existujících)
            self.step_create_organizations()

        for i, (key, name, fn) in enumerate(steps, 1):
            if only_step and key != only_step:
                continue
            if key in skip_steps:
                log_step(i, name)
                log_skip("Přeskočeno (--skip-step)")
                self.results.append((name, True, "skipped"))
                continue

            log_step(i, name)
            try:
                ok, msg = fn()
                if ok:
                    log_ok(msg)
                else:
                    log_fail(msg)
                self.results.append((name, ok, msg))
            except Exception as e:
                log_fail(str(e))
                self.results.append((name, False, str(e)))

        self._print_summary()

    def _print_summary(self):
        print(f"\n{C.BOLD}{'=' * 60}{C.END}")
        print(f"{C.BOLD}  Souhrn{C.END}")
        print(f"{'=' * 60}")
        for name, ok, msg in self.results:
            status = f"{C.OK}OK{C.END}" if ok else f"{C.FAIL}FAIL{C.END}"
            print(f"  [{status}] {name}: {msg}")

        failed = sum(1 for _, ok, _ in self.results if not ok)
        if failed:
            print(f"\n  {C.FAIL}{failed} krok(ů) selhalo{C.END}")
        else:
            print(f"\n  {C.OK}Vše OK{C.END}")

    # ── Step: Health Check ─────────────────────────────────────────────────

    def step_health_check(self) -> tuple[bool, str]:
        """Ověří dostupnost API přes actuator health nebo auth verify."""
        try:
            # Try Spring Boot actuator health
            r = self.api.get("/actuator/health")
            if r.status_code == 200:
                return True, f"API dostupné (actuator health OK)"

            # Fallback: auth verify (401 = API is running, just no auth)
            r = self.api.get("/api/auth/verify")
            if r.status_code in (200, 401, 403):
                return True, f"API dostupné (HTTP {r.status_code})"
            return False, f"API vrátilo HTTP {r.status_code}"
        except requests.ConnectionError:
            return False, f"API nedostupné na {self.api.base_url}"

    # ── Step: Register SuperAdmin ──────────────────────────────────────────

    def step_register_superadmin(self) -> tuple[bool, str]:
        """Ověří admin přístup přes dev bypass (AUTH_MODE=development)
        nebo API key. V dev mode GET /api/auth/verify projde bez tokenu
        a vrátí HOLDING_ADMIN kontext."""

        # 1) Dev bypass: GET /api/auth/verify s dummy Bearer tokenem
        #    V AUTH_MODE=development backend přeskočí JWT validaci,
        #    ale stále vyžaduje Authorization: Bearer header.
        try:
            saved_headers = dict(self.api.session.headers)
            self.api.session.headers["Authorization"] = "Bearer dev-bypass-token"
            r = self.api.get("/api/auth/verify")
            if r.status_code == 200:
                self.admin_token = "dev-bypass"
                log_info("AUTH_MODE=development — dev bypass aktivní")
                return True, "Admin přístup přes dev bypass (HOLDING_ADMIN)"
            self.api.session.headers.update(saved_headers)
        except Exception as e:
            log_warn(f"Dev bypass selhal: {e}")

        # 2) Fallback: zkusit API key z env proměnné
        api_key = os.environ.get("RA_ADMIN_API_KEY", "")
        if api_key:
            self.api.session.headers["X-API-Key"] = api_key
            try:
                r = self.api.get("/api/auth/verify")
                if r.status_code == 200:
                    self.admin_token = api_key
                    log_info("Přihlášen přes API key z RA_ADMIN_API_KEY")
                    return True, "Admin přístup přes API key"
            except Exception as e:
                log_warn(f"API key auth selhal: {e}")
            finally:
                self.api.session.headers.pop("X-API-Key", None)

        return False, (
            "Admin přístup selhal — zkontroluj:\n"
            "  1) AUTH_MODE=development v docker-compose\n"
            "  2) nebo nastav RA_ADMIN_API_KEY env proměnnou"
        )

    # ── Step: Load Organizations ──────────────────────────────────────────

    def step_create_organizations(self) -> tuple[bool, str]:
        """Načte existující organizace z DB (seedované při startu).
        Organizace se nevytvářejí přes init — jsou součástí DB migrací."""
        if not self.admin_token:
            return False, "Nemám admin token — přeskoč"

        self._fetch_org_id("")  # load all orgs
        if self.org_ids:
            for code, oid in self.org_ids.items():
                log_info(f"Organizace '{code}' → ID: {oid}")
            return True, f"Načteno {len(self.org_ids)} organizací"

        return False, "Žádné organizace nalezeny v DB"

    # ── Step: Seed Slide Metadata ──────────────────────────────────────────

    def step_seed_slide_metadata(self) -> tuple[bool, str]:
        """Nahraje slide metadata šablony (JSON) do engine-data:template přes REST API.

        Metadata definují strukturu pseudotabulek na PPTX slidech — kde hledat
        hlavičky, sloupce, oddělovače řádků. Atomizer je použije při extrakci.

        API: POST /api/query/templates/slide-metadata
        """
        meta = self.config.slide_metadata
        if not meta.enabled:
            return True, "SlideMetadata vypnutý (Enabled: false)"

        if not self.admin_token:
            return False, "Nemám admin token"

        # Resolve cesta relativně ke config souboru
        meta_dir = Path(meta.directory)
        if not meta_dir.is_absolute():
            meta_dir = Path(self._config_dir) / meta_dir if self._config_dir else meta_dir

        if not meta_dir.exists():
            return False, f"SlideMetadata adresář nenalezen: {meta_dir}"

        json_files = sorted(meta_dir.glob("*.json"))
        if not json_files:
            return True, f"Žádné JSON soubory v {meta_dir}"

        self.api.set_token(self.admin_token)

        uploaded = 0
        skipped = 0
        errors = 0

        for json_file in json_files:
            try:
                with open(json_file, "r", encoding="utf-8") as f:
                    metadata = json.load(f)

                template_name = metadata.get("name", json_file.stem)
                schema_version = metadata.get("schema_version", "unknown")

                # POST /api/query/templates/slide-metadata — vytvoří nebo aktualizuje šablonu
                r = self.api.post("/api/query/templates/slide-metadata", {
                    "name": template_name,
                    "schema_version": schema_version,
                    "definition": metadata,
                })

                if r.status_code in (200, 201):
                    data = r.json()
                    template_id = data.get("id") or data.get("template_id") or "?"
                    log_info(f"Metadata '{template_name}' ({json_file.name}) → ID: {template_id}")
                    uploaded += 1
                elif r.status_code == 409:
                    log_info(f"Metadata '{template_name}' ({json_file.name}) už existuje")

                    # Zkusit PUT pro update existující šablony
                    r2 = self.api.put(f"/api/query/templates/slide-metadata/by-name/{template_name}", {
                        "schema_version": schema_version,
                        "definition": metadata,
                    })
                    if r2.status_code in (200, 204):
                        log_info(f"Metadata '{template_name}' aktualizována")
                    else:
                        log_warn(f"Update '{template_name}' selhal: HTTP {r2.status_code}")

                    skipped += 1
                elif r.status_code == 404:
                    log_warn(f"Metadata endpoint neexistuje (404) — engine-data:template ještě nemá /slide-metadata")
                    errors += 1
                else:
                    log_warn(f"Metadata '{template_name}' selhala: HTTP {r.status_code} — {_err(r)}")
                    errors += 1
            except json.JSONDecodeError as e:
                log_warn(f"{json_file.name} — nevalidní JSON: {e}")
                errors += 1

        msg = f"Nahráno {uploaded}/{len(json_files)} metadata šablon"
        if skipped:
            msg += f", {skipped} aktualizováno"
        if errors:
            msg += f", {errors} chyb"
        return errors == 0, msg

    # ── Step: Upload Demo Plans ────────────────────────────────────────────

    def step_upload_demo_plans(self) -> tuple[bool, str]:
        """Nahraje demo plány z DemoData adresáře přes parser API."""
        demo = self.config.demo_seed
        if not demo.enabled:
            return True, "DemoSeed vypnutý (Enabled: false)"

        if not self.admin_token:
            return False, "Nemám admin token"

        # Resolve cesta relativně ke config souboru
        data_dir = Path(demo.data_directory)
        if not data_dir.is_absolute():
            data_dir = Path(self._config_dir) / data_dir if self._config_dir else data_dir

        if not data_dir.exists():
            return False, f"DemoData adresář nenalezen: {data_dir}"

        json_files = sorted(data_dir.glob("*.json"))
        if not json_files:
            return True, f"Žádné JSON soubory v {data_dir}"

        self.api.set_token(self.admin_token)

        # SuperAdmin nemá org v JWT → potřebujeme X-Tenant-Id header
        # Použijeme první dostupnou org (nebo "1" jako platform org fallback)
        default_org_id = next(iter(self.org_ids.values()), "1")

        uploaded = 0
        skipped = 0
        errors = 0

        for json_file in json_files:
            try:
                with open(json_file, "r", encoding="utf-8") as f:
                    plan_data = json.load(f)

                plan_name = plan_data.get("plan_name", json_file.stem)

                # Přiřadit org_id z org_ids mapy pokud existuje org_slug v plánu
                plan_org_slug = plan_data.get("organization_slug", "")
                tenant_id = self.org_ids.get(plan_org_slug, default_org_id) if plan_org_slug else default_org_id
                self.api.session.headers["X-Tenant-Id"] = str(tenant_id)

                r = self.api.post("/api/upload/parser/upload-json", plan_data)

                self.api.session.headers.pop("X-Tenant-Id", None)

                if r.status_code in (200, 201):
                    log_info(f"'{plan_name}' ({json_file.name}) nahrán")
                    uploaded += 1
                elif r.status_code == 409:
                    log_info(f"'{plan_name}' ({json_file.name}) už existuje")
                    skipped += 1
                else:
                    log_warn(f"'{plan_name}' ({json_file.name}) selhal: HTTP {r.status_code} — {_err(r)}")
                    errors += 1
            except json.JSONDecodeError as e:
                log_warn(f"{json_file.name} — nevalidní JSON: {e}")
                errors += 1

        msg = f"Nahráno {uploaded}/{len(json_files)} plánů"
        if skipped:
            msg += f", {skipped} přeskočeno"
        if errors:
            msg += f", {errors} chyb"
        return errors == 0, msg

    # ── Step: Seed Billing Invoices ─────────────────────────────────────

    def step_seed_billing(self) -> tuple[bool, str]:
        """Seeduje faktury přímo přes billing API (POST /api/admin/billing/seed)
        nebo SQL fallback do billing DB."""
        billing = self.config.billing_seed
        if not billing.enabled:
            return True, "BillingSeed vypnutý (Enabled: false)"

        if not billing.invoices:
            return True, "Žádné faktury k seedování"

        if not self.admin_token:
            return False, "Nemám admin token"

        self.api.set_token(self.admin_token)

        created = 0
        errors = 0
        for inv in billing.invoices:
            org_id = self.org_ids.get(inv.organization_slug, "")
            if not org_id:
                self._fetch_org_id(inv.organization_slug)
                org_id = self.org_ids.get(inv.organization_slug, "")
            if not org_id:
                log_warn(f"Faktura {inv.invoice_number} — org '{inv.organization_slug}' nenalezena")
                errors += 1
                continue

            # Spočítat subtotal, tax, total
            subtotal = sum(li.quantity * li.unit_price for li in inv.line_items)
            tax = subtotal * (inv.tax_rate / 100)
            total = subtotal + tax

            # Nastavit tenant header
            self.api.session.headers["X-Tenant-Id"] = str(org_id)

            payload = {
                "organizationId": str(org_id),
                "invoiceNumber": inv.invoice_number,
                "status": inv.status,
                "subtotal": subtotal,
                "tax": tax,
                "total": total,
                "currency": inv.currency,
                "periodStart": inv.period_start,
                "periodEnd": inv.period_end,
                "dueDate": inv.due_date,
                "paidAt": inv.paid_at or None,
                "taxRate": inv.tax_rate,
                "taxType": inv.tax_type,
                "sellerCountry": inv.seller_country,
                "buyerCountry": inv.buyer_country,
                "buyerVatId": inv.buyer_vat_id or None,
                "billingChannel": inv.billing_channel,
                "lineItems": [
                    {
                        "description": li.description,
                        "quantity": li.quantity,
                        "unitPrice": li.unit_price,
                        "amount": li.quantity * li.unit_price,
                        "period": li.period,
                    }
                    for li in inv.line_items
                ],
            }

            # Zkusit POST na seed endpoint
            r = self.api.post("/api/admin/billing/invoices/seed", payload)

            self.api.session.headers.pop("X-Tenant-Id", None)

            if r.status_code in (200, 201):
                log_info(f"Faktura {inv.invoice_number} ({inv.status}) pro {inv.organization_slug} vytvořena")
                created += 1
            elif r.status_code == 409:
                log_info(f"Faktura {inv.invoice_number} už existuje")
                created += 1
            elif r.status_code == 404:
                log_warn(f"Faktura {inv.invoice_number} — seed endpoint neexistuje (404), je třeba přidat endpoint")
                errors += 1
            else:
                log_warn(f"Faktura {inv.invoice_number} selhala: HTTP {r.status_code} — {_err(r)}")
                errors += 1

        msg = f"Vytvořeno {created}/{len(billing.invoices)} faktur"
        if errors:
            msg += f", {errors} chyb"
        return errors == 0, msg

    # ── Helpers ────────────────────────────────────────────────────────────

    def _fetch_org_id(self, slug: str):
        """Zkusí načíst org ID z hierarchického seznamu organizací."""
        try:
            r = self.api.get("/api/admin/organizations")
            if r.status_code == 200:
                data = r.json()
                self._collect_org_ids(data if isinstance(data, list) else [data] if isinstance(data, dict) else [])
        except Exception:
            pass

    def _collect_org_ids(self, nodes):
        """Rekurzivně sbírá org IDs z hierarchické struktury."""
        if not nodes:
            return
        for org in nodes:
            if not isinstance(org, dict):
                continue
            code = org.get("code", "")
            slug = org.get("slug", "")
            oid = str(org.get("id", ""))
            if oid:
                if code:
                    self.org_ids[code] = oid
                    self.org_ids[code.lower().replace("_", "-")] = oid
                if slug:
                    self.org_ids[slug] = oid
            self._collect_org_ids(org.get("children"))


def _err(r: requests.Response) -> str:
    """Extrahuje chybovou zprávu z response."""
    try:
        data = r.json()
        return data.get("message", data.get("error", data.get("title", str(data))))
    except Exception:
        return r.text[:200] if r.text else "(prázdná odpověď)"


# ── Wait for API ─────────────────────────────────────────────────────────────

def wait_for_api(api: ApiClient, timeout: int = 60):
    """Čeká na dostupnost API s timeoutem."""
    print(f"\n{C.INFO}Čekám na API ({api.base_url})...{C.END}", end="", flush=True)
    start = time.time()
    while time.time() - start < timeout:
        try:
            r = api.get("/api/auth/user/context")
            if r.status_code in (200, 401, 403):
                print(f" {C.OK}OK{C.END} ({r.status_code})")
                return True
        except requests.ConnectionError:
            pass
        print(".", end="", flush=True)
        time.sleep(2)

    print(f"\n{C.FAIL}API nedostupné po {timeout}s{C.END}")
    return False


# ── Main ─────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="RA Platform Init — nastavení přes REST API",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument(
        "--config", "-c",
        default=str(Path(__file__).parent / "init.json"),
        help="Cesta ke konfiguraci (default: init.json vedle skriptu)",
    )
    parser.add_argument(
        "--api-url", "-u",
        default=os.environ.get("RA_API_URL", "http://localhost:8081"),
        help="URL API (default: $RA_API_URL nebo http://localhost:8081 — direct to engine-core)",
    )
    parser.add_argument(
        "--wait", "-w",
        type=int, default=30,
        help="Timeout čekání na API v sekundách (default: 30)",
    )
    parser.add_argument(
        "--skip-step", "-s",
        action="append", default=[],
        help="Přeskočit krok (health, superadmin, orgs, users, azure, sso)",
    )
    parser.add_argument(
        "--only-step", "-o",
        default=None,
        help="Spustit jen jeden krok",
    )
    parser.add_argument(
        "--no-wait",
        action="store_true",
        help="Nepřeskakovat čekání na API",
    )

    args = parser.parse_args()

    # Načíst konfiguraci
    config_path = args.config
    if not os.path.exists(config_path):
        print(f"{C.FAIL}Konfigurace nenalezena: {config_path}{C.END}")
        sys.exit(1)

    config = load_config(config_path)

    print(f"{C.DIM}Config: {config_path}{C.END}")
    print(f"{C.DIM}Admin:  {config.admin.email}{C.END}")
    print(f"{C.DIM}Azure accounts: {len(config.azure_accounts)}{C.END}")
    print(f"{C.DIM}Identity providers: {len(config.identity_providers)}{C.END}")
    print(f"{C.DIM}Demo seed: {'ON' if config.demo_seed.enabled else 'OFF'} ({config.demo_seed.data_directory}){C.END}")

    # API klient
    api = ApiClient(args.api_url)

    # Počkat na API
    if not args.no_wait:
        if not wait_for_api(api, args.wait):
            print(f"\n{C.WARN}Tip: Spusť API gateway a zkus znovu, nebo použij --no-wait{C.END}")
            sys.exit(1)

    # Spustit init
    runner = InitRunner(api, config, config_dir=str(Path(config_path).parent))
    runner.run_all(
        skip_steps=set(args.skip_step),
        only_step=args.only_step,
    )

    # Exit code
    failed = sum(1 for _, ok, _ in runner.results if not ok)
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
