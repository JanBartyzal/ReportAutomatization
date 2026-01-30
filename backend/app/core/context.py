from contextvars import ContextVar
from typing import Optional

# Context variable to store the current Organization ID
# This will be set by the Authentication dependency
organization_context: ContextVar[Optional[int]] = ContextVar("organization_id", default=None)
