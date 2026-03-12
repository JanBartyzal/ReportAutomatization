-- V2: Create notification_settings table for granular notification preferences
-- This table stores per-user, per-organization notification preferences

CREATE TABLE notification_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    org_id VARCHAR(255) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    critical_only BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uq_notification_settings UNIQUE (user_id, org_id, notification_type)
);

-- Index for efficient lookups
CREATE INDEX idx_notification_settings_user_org 
    ON notification_settings(user_id, org_id);

-- Index for finding all settings for a notification type
CREATE INDEX idx_notification_settings_type 
    ON notification_settings(notification_type);

-- Row-Level Security (RLS) - handled at application level
COMMENT ON TABLE notification_settings IS 'Per-user notification preferences for email and in-app notifications';
COMMENT ON COLUMN notification_settings.user_id IS 'User ID who owns this setting';
COMMENT ON COLUMN notification_settings.org_id IS 'Organization ID context';
COMMENT ON COLUMN notification_settings.notification_type IS 'Type of notification (REPORT_SUBMITTED, FILE_PROCESSED, etc.)';
COMMENT ON COLUMN notification_settings.email_enabled IS 'Whether email notifications are enabled for this type';
COMMENT ON COLUMN notification_settings.in_app_enabled IS 'Whether in-app notifications are enabled for this type';
COMMENT ON COLUMN notification_settings.critical_only IS 'Only send critical notifications via email';
