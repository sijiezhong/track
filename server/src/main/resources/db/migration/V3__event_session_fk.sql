DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.table_constraints tc
    WHERE tc.table_schema = 'public'
      AND tc.table_name = 'event'
      AND tc.constraint_type = 'FOREIGN KEY'
      AND tc.constraint_name = 'fk_event_session'
  ) THEN
    ALTER TABLE event
      ADD CONSTRAINT fk_event_session
      FOREIGN KEY (session_id) REFERENCES session (id) ON DELETE SET NULL;
  END IF;
END $$;
