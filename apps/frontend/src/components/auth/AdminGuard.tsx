import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { Role } from '@reportplatform/types';
import LoadingSpinner from '../LoadingSpinner';

interface AdminGuardProps {
    children: React.ReactNode;
    allowedRoles?: Role[];
}

/**
 * AdminGuard component
 * Protects routes by checking if the user has required admin roles.
 * Defaults to HOLDING_ADMIN or ADMIN.
 */
export const AdminGuard: React.FC<AdminGuardProps> = ({ 
    children, 
    allowedRoles = [Role.HOLDING_ADMIN, Role.ADMIN] 
}) => {
    const { data: user, isLoading } = useAuth();
    const location = useLocation();

    if (isLoading) {
        return <LoadingSpinner />;
    }

    if (!user) {
        return <Navigate to="/login" state={{ from: location }} replace />;
    }

    const hasAccess = (user.roles || []).some(role => allowedRoles.includes(role));

    if (!hasAccess) {
        console.warn(`[AdminGuard] Access denied for user ${user.email}. Required roles: ${allowedRoles.join(', ')}`);
        return <Navigate to="/dashboard" replace />;
    }

    return <>{children}</>;
};

export default AdminGuard;
