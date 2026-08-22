-- Add a generic link column to notifications so the client can navigate
-- directly to the relevant screen when the notification is clicked.

ALTER TABLE notifications
    ADD COLUMN link VARCHAR(255) NULL AFTER message;
