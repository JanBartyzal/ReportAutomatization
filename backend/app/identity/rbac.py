
from typing import List, Optional
from enum import Enum
from fastapi import Depends, HTTPException, status
from app.schemas.user import User
from app.identity.auth import get_current_user
import logging

# Logger for Audit
audit_logger = logging.getLogger("audit-log")

class UserRole(str, Enum):
    ADMIN = "ADMIN"
    EDITOR = "EDITOR"
    VIEWER = "VIEWER"

def has_role(user: User, allowed_roles: List[UserRole]) -> bool:
    """
    Checks if user has ANY of the allowed roles.
    """
    if not user.roles:
        return False
        
    for user_role in user.roles:
        # Case-insensitive comparison and fallback for string matches
        normalized_role = user_role.upper()
        if normalized_role in [r.value for r in allowed_roles]:
            return True
            
    return False

class RoleChecker:
    def __init__(self, allowed_roles: List[UserRole]):
        self.allowed_roles = allowed_roles

    def __call__(self, user: User = Depends(get_current_user)):
        if not has_role(user, self.allowed_roles):
            audit_logger.warning(f"AUDIT [FORBIDDEN]: User {user.id} ({user.roles}) tried to access protected resource needed {self.allowed_roles}")
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN, 
                detail="Operation not permitted"
            )
        
        # Audit Log for Admin actions (if role is ADMIN/EDITOR)
        # Note: Ideally we log specific operation, but dependency doesn't know route easily without Request
        # We assume success if no exception raised.
        # audit_logger.info(f"AUDIT [SUCCESS]: User {user.id} accessed resource.")
        return user

# Convenience dependency for Admin-only routes
verify_admin = RoleChecker([UserRole.ADMIN])

