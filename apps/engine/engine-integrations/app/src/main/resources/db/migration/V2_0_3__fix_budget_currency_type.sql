-- V2_0_3: Fix budget_currency column type
-- CHAR(3) (bpchar) causes Hibernate schema validation failure — change to VARCHAR(3).
ALTER TABLE snow_project_sync_config
    ALTER COLUMN budget_currency TYPE VARCHAR(3);
