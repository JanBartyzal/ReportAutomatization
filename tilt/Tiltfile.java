# =============================================================================
# Tiltfile.java - Java Service Configuration (P8 Consolidated)
# =============================================================================
# Consolidated Java services + standalone services
# =============================================================================

# ---------------------------------------------------------------------------
# Consolidated Java Services
# ---------------------------------------------------------------------------
# (service_name, extra_deps_beyond_postgres)
CONSOLIDATED_JAVA_SERVICES = [
    ("engine-core",          []),
    ("engine-data",          ["redis"]),
    ("engine-reporting",     ["azurite"]),
    ("engine-orchestrator",  ["redis"]),
    ("engine-integrations",  ["redis"]),
]

# Standalone Java services (not consolidated)
STANDALONE_JAVA_SERVICES = [
    ("ms-ing",       ["azurite", "clamav"]),
    ("ms-scan",      ["clamav", "azurite"]),
]

# ---------------------------------------------------------------------------
# Apply labels and dependencies
# ---------------------------------------------------------------------------
for svc, extra_deps in CONSOLIDATED_JAVA_SERVICES:
    deps = ["postgres"] + extra_deps
    dc_resource(svc, labels=["core-java"], resource_deps=deps)

for svc, extra_deps in STANDALONE_JAVA_SERVICES:
    deps = ["postgres"] + extra_deps
    dc_resource(svc, labels=["core-java"], resource_deps=deps)
