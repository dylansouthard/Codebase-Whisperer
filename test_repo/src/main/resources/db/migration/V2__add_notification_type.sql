-- Add column with a default so existing rows pass NOT NULL check fast
ALTER TABLE notifications
    ADD COLUMN type varchar(255) NOT NULL DEFAULT 'UPDATE';

-- Constrain allowed values
ALTER TABLE notifications
    ADD CONSTRAINT notifications_type_chk
        CHECK (type IN ('ALERT','UPDATE'));
