from fastapi import APIRouter, Depends
from core.schemas import UserMeResponse, UserPermissions, User
from core.identity.auth import get_current_user
from core.identity.rbac import UserRole, has_role

router = APIRouter()

@router.get("/")
def read_root():
    return {"Hello": "Identity Service"}

@router.get("/health")
def health_check():
    return {"status": "ok"}

@router.get("/me", response_model=UserMeResponse)
async def get_my_profile(current_user: User = Depends(get_current_user)):
    # Calculate permissions
    permissions = UserPermissions(
        can_sync_prices=has_role(current_user, [UserRole.ADMIN]),
        can_edit_plans=has_role(current_user, [UserRole.ADMIN, UserRole.EDITOR]),
        can_view_reports=True # Viewer and up can view
    )
    
    return UserMeResponse(
        id=current_user.id,
        email=current_user.email,
        roles=current_user.roles,
        permissions=permissions
    )
